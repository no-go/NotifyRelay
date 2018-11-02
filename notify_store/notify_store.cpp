#include <arpa/inet.h>
#include <sys/socket.h>
#include <string>
#include <cstring> // memset
#include <cstdlib>
#include <cstdio>
#include <unistd.h>

#include <fstream>
#include <iostream>
#include <sstream>

#define BUFLEN          10000
#define DEFAULT_PORT    58000
#define MAXLINES        6

bool nonDaemon = true;
std::string filename;

void daemonProcess(int port) {
    struct sockaddr_in si_me, si_other;
    
    int s, newsockfd;
    socklen_t recv_len;
    unsigned int slen = sizeof(si_other);
    s = socket(AF_INET, SOCK_STREAM, 0);
    if (s < 0) {
        printf("ERROR opening socket\n");
        return;
    }
    memset((char *) &si_me, 0, sizeof(si_me));
    si_me.sin_family = AF_INET;
    si_me.sin_port = htons(port);
    si_me.sin_addr.s_addr = htonl(INADDR_ANY);

    if (bind(s, (struct sockaddr*)&si_me, sizeof(si_me)) < 0) {
        printf("ERROR on binding\n");
        return;
    }
    
    printf("listening ...\n");

    while(1==1) {
        char buf[BUFLEN] = {0};
        listen(s, 5);
        newsockfd = accept(s, (struct sockaddr *) &si_other, &slen);
        recv_len = read(newsockfd, buf, BUFLEN);
        std::string ipAddr = inet_ntoa(si_other.sin_addr);
        if (recv_len > 0) {
            std::string line;
            std::ifstream infile;
            infile.open(filename);
            std::stringstream ss;
            ss << buf;
            if (infile.is_open()) {
                int i = 0;
                while (getline(infile, line) && i < MAXLINES) {
                    ss << line << '\n';
                    //if (nonDaemon) printf("line %d: %s\n", i, line.c_str());
                    i++;
                }
                infile.close();
            }
            if (nonDaemon) printf("%s: %s\n", ipAddr.c_str(), buf);
            std::ofstream offile;
            offile.open(filename);
            if (offile.is_open()) {
                offile << ss.str();
                offile.close();
            }
        }
        close(newsockfd);
    }
    // never reached ?!
    close(s);
    printf("daemon killed ?!\n");
}

int main(int argc, char *argv[]) {
    int port = DEFAULT_PORT;

    if (argc > 2) {
        printf("Hint Usages\n===========\n"
            "on default port, listen as non-daemon:  %s [file]\n"
            "other port, listen as NON-daemon:       %s [file] [port]\n"
            "other port, listen as daemon:           %s [file] [port] -d\n\n",
            argv[0],argv[0],argv[0]
        );
        filename = argv[1];
        port = std::atoi(argv[2]);
        if (argc > 3) {
            std::string op1 = argv[3];
            if (op1 == (std::string) "-d") nonDaemon = false;
        }
    }

    if (nonDaemon) {
        printf("=> running as non-daemon\n");
        daemonProcess(port);
        return 0;
    }

    pid_t pid;
    pid = fork();
    if (pid < 0) {
        return 1;
    }
    if (pid > 0) {
        printf("=> daemon started (pid = %d)\n", pid);
        return 0;
    }
    daemonProcess(port);
    return 0;
}
