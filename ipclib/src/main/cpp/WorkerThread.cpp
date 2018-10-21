//
// Created by dbond on 20.10.18.
//

#include "WorkerThread.h"
#include <android/log.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <string.h>
#include <algorithm>
#include <time.h>
#include <signal.h>
#include <stdio.h>
//#include <sys/un.h>
# define SUN_LEN(ptr) ((size_t) (((struct sockaddr_un *) 0)->sun_path)	      \
		      + strlen ((ptr)->sun_path))
#include <android/sharedmem.h>



#define ASHMEM_NAME_LEN         256
#define __ASHMEMIOC             0x77
#define ASHMEM_SET_NAME         _IOW(__ASHMEMIOC, 1, char[ASHMEM_NAME_LEN])
#define ASHMEM_SET_SIZE         _IOW(__ASHMEMIOC, 3, size_t)
#define BUFFER_SIZE 1024

static const char* kTAG = "WorkerThread";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))


WorkerThread::~WorkerThread() {
    stop();
}

int WorkerThread::start(){
    LOGW("WorkerThread::start()");
    if (!keepRun.load(std::memory_order_acquire)) {
        stop();
//        mainThread= std::thread(&WorkerThread::mainThreadLoop, this);
//        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    l_strsize = _strsize;
    l_appname = _appname;
    l_shmname = _shmname;
    keepRun.store(true, std::memory_order_release);
    startShared();
    startALoop();
    startSock();
    return 1;
}

void WorkerThread::stop(){
    LOGW("WorkerThread::stop()");
    keepRun.store(false, std::memory_order_release);

//    if (mainThread.joinable()) {
//        mainThread.join();
//    }
    stopShared();
    stopSock();
    stopALoop();
}

long long WorkerThread::getLastActTime(){
    return lastAct.load(std::memory_order_acquire);
}

long long WorkerThread::getCurJavaTime() {
    struct timespec timeout;
    clock_gettime(0, &timeout);
    return (long long)((timeout.tv_sec) * 1000LL + timeout.tv_nsec/1000000);
}

void* WorkerThread::mainThreadLoop(void* arg) {
    WorkerThread* p = reinterpret_cast<WorkerThread*>(arg);
    p->mainThreadRun();
    return 0;
}

int WorkerThread::getThreadState() {
    if (keepRun.load(std::memory_order_acquire)) { return 1;}
    return 0;
}

void WorkerThread::init(const char * c_appname, const char * c_shmname,
                        const char * c_appname2, int strsize) {
    std::lock_guard<std::mutex> lg(m_config);
    _appname = std::string(c_appname);
    _appname2 = std::string(c_appname2);
    _shmname = std::string(c_shmname);
    _strsize = strsize;
}

void WorkerThread::mainThreadRun(){
    LOGW("WorkerThread::mainThreadRun() - start");
    {
        std::lock_guard<std::mutex> lg(m_config);
        l_strsize = _strsize;
        l_appname = _appname;
        l_shmname = _shmname;
    }


    startShared();
    startSock();


    while(keepRun.load(std::memory_order_acquire)){
        _sendShared();
        _getShared();
        lastAct.store(getCurJavaTime() , std::memory_order_release);
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    LOGW("WorkerThread::mainThreadRun() - stop");
    stopShared();
    stopSock();
}

int WorkerThread::looperCallback(int fd, int events, void* data) {
    WorkerThread* p = reinterpret_cast<WorkerThread*>(data);
    char msg[BUFFER_SIZE];
    memset(msg, 0, BUFFER_SIZE);
    read(fd, msg, BUFFER_SIZE-1); // read message from pipe
    LOGW("got message #%s", msg);
    std::string str (msg);
    if (!str.empty()){
        std::lock_guard<std::mutex> lg(p->m_strALooper);
        p->strALooper = str;
    }

    return 1; // continue listening for events
}

bool WorkerThread::startALoop(){
    bool re = false;
    //faux loop
    do{
        //mainThreadLooper = ALooper_forThread();
        mainThreadLooper = ALooper_prepare(0);
        if (!mainThreadLooper) {

            break;
        }
        ALooper_acquire(mainThreadLooper); // add reference to keep object alive
        if (-1==pipe(messagePipe)) { break; }
        //messagePipe[0]-for read:
        if (-1==ALooper_addFd(mainThreadLooper, messagePipe[0],
                      0, ALOOPER_EVENT_INPUT, looperCallback, this)){
            break;
        }
        //messagePipe[1]-for write:
        alooperFd.store(messagePipe[1], std::memory_order_release);
        re = true;
    }while (false);
    if (!re) {
        keepRun.store(false, std::memory_order_release);
    }
    return re;
}

void WorkerThread::stopALoop(){
    if (mainThreadLooper) {
        ALooper_removeFd(mainThreadLooper, messagePipe[0]);
        ALooper_release(mainThreadLooper);
        mainThreadLooper = nullptr;
        alooperFd.store(-1, std::memory_order_release);
        close (messagePipe[0]);
        messagePipe[0] = -1;
        close (messagePipe[1]);
        messagePipe[1] = -1;

    }
}

bool WorkerThread::startShared(){
    bool allOk = false;
    //faux loop
    do {
        int fd = -1;
#if __ANDROID_API__ >= __ANDROID_API_O__
        fd = ASharedMemory_create(l_shmname.c_str(), l_strsize);
#else
        fd = open("/dev/ashmem", O_RDWR);
        if (-1==fd) {break;}
        ioctl(fd, ASHMEM_SET_NAME, l_shmname.c_str());
        ioctl(fd, ASHMEM_SET_SIZE, l_strsize);
#endif
        if (-1==fd) {break;}
        sharedFd.store(fd, std::memory_order_release);
//        shmap = (char *) mmap(NULL, l_strsize, PROT_READ | PROT_WRITE, MAP_SHARED, sharedFd, 0);
//        if (!shmap) {break;}
        allOk = true;
    } while (false);
    if (!allOk) {
        keepRun.store(false, std::memory_order_release);
    }
    return allOk;
}

void WorkerThread::startSock(){
    sockThread= std::thread(&WorkerThread::sockThreadLoop, this);
}

void WorkerThread::stopShared(){
//    if (shmap) {
//        munmap(shmap, l_strsize);
//        shmap = nullptr;
//    }

    if (sharedFd.load(std::memory_order_acquire)>=0) {
        close(sharedFd.load(std::memory_order_acquire));
        sharedFd.store(-1, std::memory_order_release);
    }

}

void WorkerThread::stopSock(){
    std::string res =sendCmdS(l_appname.c_str(), "TERMINATE");
    LOGW("WorkerThread::stopSock(): %s", res.c_str());
    if (sockThread.joinable()) {
        sockThread.join();
    }
}

void WorkerThread::sendALoop(const char * str) {
    //faux loop
    do {
        int remoteFd = getFD("GETALOOPERFD");
        if (remoteFd<0) { break; }
        /* hardcode packet size to keep parse simple */
        char msg[BUFFER_SIZE];
        memset(msg, 0, BUFFER_SIZE);
        int len = strlen(str);
        if (len>BUFFER_SIZE-1) { len = BUFFER_SIZE-1;}
        memcpy(msg, str, len);
        write(remoteFd, msg, BUFFER_SIZE);
    } while (false);
}

std::string WorkerThread::getALoop(){
        std::lock_guard<std::mutex> lg(m_strALooper);
        return strALooper;
}

void WorkerThread::sendShared(int8_t * array, int len) {
    //std::lock_guard<std::mutex> lg(m_queSendShared);
    queSendShared.push(std::make_shared<std::vector<int8_t>>(array,array+len));

    _sendShared();
}

int WorkerThread::getFD(const char * who) {
    int re = -1;
    //faux loop
    do {
        const std::string & res = sendSock(who);
        if (!res.empty()) {
            re = unPackFD(res.c_str(), res.length());
        }

    } while(false);
    return re;
}

void WorkerThread::_sendShared(){
    //faux loop
    do {
        std::shared_ptr<std::vector<int8_t>> p_vec;
        {
            //std::lock_guard<std::mutex> lg(m_queSendShared);
            if (queSendShared.empty()) { break; }
            p_vec = queSendShared.front();
            queSendShared.pop();
        }

        int remoteFd = getFD("GETSHAREDMEMFD");
        if (remoteFd<0) { break; }
        char * shmap = (char *) mmap(NULL, l_strsize, PROT_READ | PROT_WRITE, MAP_SHARED, remoteFd, 0);
        if (!shmap) { break; }
        std::vector<int8_t> *vec = p_vec.get();
        for (int i = 0; i < vec->size() && i < l_strsize; ++i) {
            shmap[i] = vec->at(i);
        }
        munmap(shmap, l_strsize);
    } while (false);

}

void WorkerThread::_getShared() {
    //faux loop
    do {
        int remoteFd = getFD("GETSHAREDMEMFD");
        if (remoteFd<0) { break; }
        char * shmap = (char *) mmap(NULL, l_strsize, PROT_READ | PROT_WRITE, MAP_SHARED, remoteFd, 0);
        if (!shmap) { break; }
        std::shared_ptr<std::vector<int8_t>> p_vec =
                std::make_shared<std::vector<int8_t>>();
        std::vector<int8_t> *vec = p_vec.get();
        vec->reserve(l_strsize);
        for (int i = 0; i < l_strsize; ++i) {
            vec->push_back(shmap[i]);
        }
        {
            std::lock_guard<std::mutex> lg(m_vecShared);
            vecShared = p_vec;
        }
        munmap(shmap, l_strsize);

    } while (false);

}

std::shared_ptr<std::vector<int8_t>> WorkerThread::getShared(){
    //std::lock_guard<std::mutex> lg(m_vecShared);
    //return vecShared;
    _getShared();
    return vecShared;
}


void* WorkerThread::sockThreadLoop(void* arg){
    WorkerThread* p = reinterpret_cast<WorkerThread*>(arg);
    p->sockThreadRun();
    return 0;
}

void WorkerThread::sockThreadRun(){

    //const std::string &sock_path = LinuxSystem::getSockPathS(SPEC_SERVICE);
    int connection_socket = -1;
    struct sockaddr_un name;
    //faux loop
    bool allSystemsStarted = false;
    do {
        /* Start context: */

        /* Unix socket for cmd receiving */

        int ret;
        int data_socket;
        char buffer[BUFFER_SIZE];

        /* chmode 0777 all files i'll create: */
        umask(0);

        /*
        * In case the program exited inadvertently on the last run,
        * remove the socket.
        */
//        if (file_exists(l_appname.c_str())) {
//            unlink(l_appname.c_str());
//            remove(l_appname.c_str());
//        }


        /* Create local socket. */

        connection_socket = socket(AF_UNIX, SOCK_SEQPACKET, 0);
        if (connection_socket == -1) {
            LOGE("-1 =socket(AF_UNIX, : %s", strerror(errno));
            break;
        }

        memset(&name, 0, sizeof(struct sockaddr_un));

        /* Bind socket to socket name. */

        name.sun_family = AF_UNIX;
        memcpy(name.sun_path+1, l_appname.c_str(),
               l_appname.length()>UNIX_PATH_MAX?UNIX_PATH_MAX:l_appname.length());
        ret = bind(connection_socket, (const struct sockaddr *) &name,
                   sizeof(struct sockaddr_un));
        if (ret == -1) {
            LOGE("-1 = bind(connection_socket,: %s", strerror(errno));
            break;
        }

        /*
        * Prepare for accepting connections. The backlog size is set
        * to 20. So while one request is being processed other requests
        * can be waiting.
        */

        ret = listen(connection_socket, 20);
        if (ret == -1) {
            LOGE("-1 =listen(connection_socket: %s", strerror(errno));
            break;
        }

        /* chmode 0755 all files i'll create: */
        umask(022);


        allSystemsStarted = true;
        /* This is the main loop for handling connections. */
        while (keepRun.load(std::memory_order_acquire)) {
            /* Wait for incoming connection. */
            data_socket = accept(connection_socket, NULL, NULL);
            if (data_socket == -1) { continue; }
            /* Read cmd */
            while (keepRun.load(std::memory_order_acquire)) {
                ret = read(data_socket, buffer, BUFFER_SIZE);
                if (ret < 0)  { break; }
                if (ret == 0) { continue; }
                buffer[ret] = 0;
                std::string str (buffer);
                if (!strncmp(buffer, "TERMINATE", 9)) {
                    keepRun.store(false, std::memory_order_release);
                } else if (!strncmp(buffer, "GETSHAREDMEMFD", 14)) {
                    str = packFd("SHAREDMEMFD",
                                 sharedFd.load(std::memory_order_acquire));
                } else if (!strncmp(buffer, "GETALOOPERFD", 12)) {
                    str = packFd("ALOOPERFD",
                                 alooperFd.load(std::memory_order_acquire));
                }
                {
                    std::lock_guard<std::mutex> lg(m_strSock);
                    strSock = str;
                }
                write(data_socket, str.c_str(), str.length());
                break;
            } //read while (sr.keepRun
            close(data_socket);
        } //connection while (sr.keepRun

    } while(false);

    if (!allSystemsStarted) {
        LOGE("FAIL WorkerThread::sockThreadRun..");
        keepRun.store(false, std::memory_order_release);
    }

    if (-1!=connection_socket) {
        close(connection_socket);
    }
    /* Unlink the socket. */
    //unlink(name.sun_path);
    //unlink(l_appname.c_str());
//    remove(l_appname.c_str());
    LOGW("stopped WorkerThread::sockThreadRun..");
}

std::string WorkerThread::sendCmdS(const char * serviceName, const char * cmd) {
    std::string re;
    int sock = -1;
    //faux loop
    do {
        //int sock = ::socket(PF_UNIX, SOCK_STREAM, 0);
        sock = ::socket(AF_UNIX, SOCK_SEQPACKET, 0);
        if (-1 == sock) {
            LOGE("-1 =sendCmdS.socket(AF_UNIX, SOCK_SEQPACKET: %s", strerror(errno));
            break;
        }
        struct sockaddr_un addr;
        memset(&addr, 0, sizeof(struct sockaddr_un));
        addr.sun_family = AF_UNIX;

        memcpy(addr.sun_path+1, serviceName, strlen(serviceName));

        int res = connect(sock, (const struct sockaddr *) &addr, sizeof(struct sockaddr_un));
        if (-1 == res ) {
            LOGE("-1 =sendCmdS.connect(sock, (struct : %s", strerror(errno));
            break;
        }

        /* Send. */
        res = write(sock, cmd, strlen(cmd) + 1);
        if (-1 == res ) {
            LOGE("-1 =sendCmdS.write(sock,  : %s", strerror(errno));
            break;
        }

        char buffer[BUFFER_SIZE];
        /* Receive result. */
        res = read(sock, buffer, BUFFER_SIZE);
        if ( res < 0 ) {
            LOGE("-1 =sendCmdS.read(sock, buffer: %s", strerror(errno));
            break;
        }

        //buffer[res] = 0;
        re = std::string(buffer, res);
        //printf("Server answer: %s\n", buffer);

        //re = true;
    } while (false);
    if (-1 != sock) { ::close(sock); }

    return re;
}

bool WorkerThread::file_exists(const char * path) {
    struct stat fileStat;
    if ( stat(path, &fileStat) )
    {
        return false;
    }
    if ( !S_ISREG(fileStat.st_mode) )
    {
        return false;
    }
    return true; //Есть такой и это файл
}

std::string WorkerThread::getSock(){
     std::lock_guard<std::mutex> lg(m_strSock);
     return  strSock;

}

std::string WorkerThread::sendSock(const char * str){
    const std::string& res =sendCmdS(_appname2.c_str(), str);
    /*Self send:*/
    //const std::string& res =sendCmdS(_appname.c_str(), str);
    LOGW("Sended: %s", res.c_str());
    return res;
}

int WorkerThread::unPackFD (const char * first, int len) {
    const char * last = first + len;
    int re = 0;
    const char * ch = first;
    for (;*ch!='<' && ch<last;++ch){}
    if (*ch!='<' || ch==last ) { return -1;}
    ++ch;
    if (*ch<'0' || *ch>'9') { return -1;}
    for (;*ch!='>' && *ch && ch<last; ++ch) {
        if (*ch<'0' || *ch>'9') { break;}
        re = 10 * re + *ch - '0';
    }
    return re>LLONG_MAX?LLONG_MAX:re;
}



std::string WorkerThread::packFd(const char * prefix, int fd){
    std::string pack(prefix);
    pack.push_back('<');
    if (fd>=0) {
        if (0 == fd) {
            pack.push_back('0');
        } else {
            char buf[24];
            char *ch = buf;
            int n1 = fd;
            while (0 != n1) {
                *ch = n1 % 10 + '0';
                ++ch;
                n1 = n1 / 10;
            }
            --ch;
            while (ch >= buf) {
                pack.push_back(*ch);
                --ch;
            }
        }
    }
    pack.push_back('>');
    return pack;
}