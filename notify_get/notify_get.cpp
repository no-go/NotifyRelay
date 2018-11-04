#include <libnotify/notify.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <string>
#include <cstring> // memset
#include <cstdlib>
#include <cstdio>
#include <sstream>
#include <netdb.h>
#include <vector>
#include <netinet/in.h>
#include <algorithm>
#include <fstream>
#include <locale>

#define SPLITTOKEN      " || "
#define SPLITTOKEN_LEN  4
#define PHRASESFILE     "/phrase"
#define IGNORELINEAFTER 50000
#define BUFFER          3

std::string INIVECTOR = "3262737X857900719147446620464";
std::string pass      = "abc123"; // @todo fill this in a other way!


std::string hoststr;
int port;
int refreshSec;
std::string get;
long unsigned maxlines = 0;
std::locale loc;
bool debugmode = false;

std::string execPath;

void extractNew(std::string header, std::vector<std::string> & datas) {
    // @todo extract "Last-Modified: Sun, 21 Oct 2018 09:38:04 GMT" ??
    
    static std::vector<std::string> oldDatas;
    if (oldDatas.size() == 0) {
        //printf("init oldDatas first time\n");
        for (auto l : datas) {
            oldDatas.push_back(l);
        }
    }
    for (auto l : oldDatas) {
        // if a message has a timestamp, the coded (new) message should really be different
        datas.erase(std::remove(datas.begin(), datas.end(), l), datas.end());
    }
    // add new ones to top:
    std::reverse(oldDatas.begin(), oldDatas.end());
    for (std::string l : datas) {
        oldDatas.push_back(l);
    }
    std::reverse(oldDatas.begin(), oldDatas.end());
    
    // may remove old content at the end
    if (oldDatas.size() > maxlines) oldDatas.resize(maxlines);
}

void strangeDecode(std::vector<std::string> & datas) {

    std::ifstream myfile;
    std::string here = execPath;
    here += PHRASESFILE;
    myfile.open(here.c_str());
    std::vector< std::vector<std::string> > phrases;
    std::vector<std::string> part;
    if (myfile.is_open()) {
        std::string line;
        int i=1;
        while ( std::getline(myfile, line) ) {
            part.push_back(line);
            if (i%17 == 0) { // every 17. line is a ~
                phrases.push_back(part);
                part.resize(0);
            }
            i++;
        }
        phrases.push_back(part);
        myfile.close();
    } else {
        printf("Unable to open file\n");
        exit(1);
    }
    
    for (auto & l : datas) {
        int partid = 0;
        long unsigned processed = 0;
        std::stringstream ss;
        std::stringstream decoded;
        int val = 0;
        decoded << "";
        int loopstop = 0;

        while (processed < l.length() && loopstop <= IGNORELINEAFTER) {
            loopstop++;
            
            for (int i=0; i<16; ++i) {
                std::string sub = l.substr(processed);
                if (sub.find("\n") > 0) {
                    if (debugmode) printf("'%s' starts with '%s' ?\n", sub.substr(0, 4).c_str(), phrases[partid%6][i].c_str());
                    if ( sub.find(phrases[partid%6][i]) == 0) {
                        if (i < 10) {
                            ss << (char) (i + '0');
                        } else {
                            ss << (char) (i-10 + 'A');
                        }
                        // leng is never 0
                        long unsigned leng = ss.str().length();
                        
                        if (leng%2 == 0) {
                            val += i;
                            char passc = pass[((leng/2)-1) % pass.length()];
                            char addy;
                            if ((leng/2 -1) < INIVECTOR.length()) {
                                addy = INIVECTOR[(leng/2)-1];
                            } else {
                                addy = decoded.str()[(leng/2) -1 - INIVECTOR.length()];
                            }
                            //printf("%02X xor %02X xor %02X", (char) val, passc, addy);
                            char tmp = val;
                            tmp ^= passc;
                            tmp ^= addy;
                            decoded << tmp;
                        } else {
                            val = 16*i;
                        }
                        
                        processed += phrases[partid%6][i].length();
                        i=16;
                    }
                } else {
                    i=16;
                    processed = l.length();
                }
            }
            partid++;
        }

        l = decoded.str();
        if (debugmode) printf("%s\n", decoded.str().c_str());
    }
}

void notifyThis(std::string title, std::string msg, int usec) {
    NotifyNotification *notify;
    notify_init("click.dummer.notify_get");
    std::string here = execPath;
    here += "/icon.png";
    notify = notify_notification_new(title.c_str(), msg.c_str(), here.c_str());
    notify_notification_set_timeout(notify, usec); // -1 for ever?!
    notify_notification_show(notify, nullptr);
    g_object_unref(G_OBJECT (notify));
    notify_uninit();
}

void daemonProcess(void) {
    struct sockaddr_in client;

    struct hostent * host = gethostbyname(hoststr.c_str());

    if ( (host == NULL) || (host->h_addr == NULL) ) {
        printf("ERROR retrieving DNS information\n");
        return;
    }

    memset((char *) &client, 0, sizeof(client));
    client.sin_family = AF_INET;
    client.sin_port = htons(port);
    memcpy(&client.sin_addr, host->h_addr, host->h_length);
    
    std::stringstream ss;
    ss  << "GET " << get << " HTTP/1.1\r\n"
        << "Host: " << hoststr << "\r\n"
        << "Accept: application/json\r\n"
        << "\r\n\r\n";
    std::string request = ss.str();

    while (1==1) {
        int s = socket(AF_INET, SOCK_STREAM, 0);
        if (s < 0) {
            printf("ERROR opening socket\n");
            return;
        }
        
        if (connect(s, (struct sockaddr *)&client, sizeof(client)) < 0) {
            close(s);
            printf("ERROR Could not connect\n");
            return;
        }
        
        if (send(s, request.c_str(), request.length(), 0) != (int)request.length()) {
            close(s);
            printf("ERROR sending request\n");
            return;
        }

        char cur;
        int leng=0;
        std::vector<std::string> datas;
        std::stringstream dummy;
        std::string header;
        bool headerend = false;
        while ( read(s, &cur, 1) > 0 ) {
            dummy << cur;
            leng++;
            if (headerend && cur == '\n') {
                datas.push_back(dummy.str());
                dummy.str("");
            }
            if (
                !headerend &&
                leng > 9 && 
                dummy.str()[leng-9] == 'p' && 
                dummy.str()[leng-8] == 'l' && 
                dummy.str()[leng-7] == 'a' && 
                dummy.str()[leng-6] == 'i' && 
                dummy.str()[leng-5] == 'n' && 
                dummy.str()[leng-4] == '\r' && 
                dummy.str()[leng-3] == '\n' && 
                dummy.str()[leng-2] == '\r' && 
                dummy.str()[leng-1] == '\n'
            ) {
                header = dummy.str();
                headerend = true;
                dummy.str("");
            }
        }
        close(s);
        maxlines = datas.size() + BUFFER;
        if (debugmode) printf("%ld lines readed\n", datas.size());
        //for (auto l : datas) printf("     %s\n", l.c_str());
        extractNew(header, datas);
        strangeDecode(datas);
        for (auto l : datas) {
            std::string title = l.substr(0, l.find(SPLITTOKEN));
            std::string msg = l.substr(l.find(SPLITTOKEN)+SPLITTOKEN_LEN);
            notifyThis(title, msg, 50000);
        }
        sleep(refreshSec);
    }
    // never reached ?!
    printf("daemon killed\n");
}

int main(int argc, char *argv[]) {
    std::string dummy = realpath(argv[0], nullptr);
    execPath = dummy.substr(0, dummy.rfind("/"));
    if (argc < 5) {
        printf("Hint Usages\n===========\n"
            "%s [host] [port] [get] [refresh sec]\n"
            "%s [host] [port] [get] [refresh sec] debug\n",
            argv[0], argv[0]
        );
        return -1;
    }
    hoststr = argv[1];
    port = std::atoi(argv[2]);
    get = argv[3];
    refreshSec = std::atoi(argv[4]);
    notifyThis("Notify Get", "I am started!", 10000);
    if (argc == 6) {
        debugmode = true;
        daemonProcess();
        return 0;
    }

    pid_t pid;
    pid = fork();
    if (pid < 0) {
        // fail
        return 1;
    }
    if (pid > 0) {
        // I am parent of fork with that pid
        printf("=> daemon started (pid = %d)\n", pid);
        return 0;
    }
    // fork with pid=0 is running daemon

    while (1==1) {
        daemonProcess();
        // fallback if something went wrong
        sleep(refreshSec);
    }
    return 0;
}
