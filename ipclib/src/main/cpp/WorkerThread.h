//
// Created by dbond on 20.10.18.
//

#ifndef IPC_NDK_WORKERTHREAD_H
#define IPC_NDK_WORKERTHREAD_H


#include <atomic>
#include <thread>
#include <string>
#include <queue>
#include <android/looper.h>

class WorkerThread {
public:
    virtual ~WorkerThread();
    void init(const char * c_appname, const char * c_shmname,
              const char * c_appname2, int strsize);
    int start();
    void stop();
    long long getLastActTime();
    long long getCurJavaTime();
    int getThreadState();
    void sendShared(int8_t * array, int len);
    std::shared_ptr<std::vector<int8_t>> getShared();
    std::string sendSock(const char * str);
    std::string getSock();
    void sendALoop(const char * str);
    std::string getALoop();

private:
    std::mutex m_config;
    std::string _appname;
    std::string _appname2;
    std::string _shmname;
    int _strsize;
    ALooper* mainThreadLooper;
    int messagePipe[2];

    std::atomic<bool> keepRun {false};
    std::atomic<long long> lastAct {0};
    std::atomic<int> sharedFd {-1};
    std::atomic<int> alooperFd {-1};

    //std::mutex m_queSendShared;
    std::queue<std::shared_ptr<std::vector<int8_t>>> queSendShared;

    std::mutex m_vecShared;
    std::shared_ptr<std::vector<int8_t>> vecShared;

    std::mutex m_strSock;
    std::string strSock;

    std::mutex m_strALooper;
    std::string strALooper;

    std::thread mainThread;
    std::thread sockThread;

    static void* mainThreadLoop(void* arg);
    void mainThreadRun();
    static void* sockThreadLoop(void* arg);
    void sockThreadRun();

    bool startShared();
    void startSock();
    bool startALoop();
    void stopALoop();
    void stopShared();
    void stopSock();
    void _sendShared();
    void _getShared();
    std::string sendCmdS(const char * serviceName, const char * cmd);
    bool file_exists(const char * path);
    std::string packFd(const char * prefix, int fd);
    int unPackFD(const char * first, int len);
    int getFD(const char * who);
    static int looperCallback(int fd, int events, void* data);

    /* mainThread thread stuff */
    int l_strsize {0};

    std::string l_appname;
    std::string l_shmname;
};


#endif //IPC_NDK_WORKERTHREAD_H
