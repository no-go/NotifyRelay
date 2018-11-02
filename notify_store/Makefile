NAME = notify_store
CXX = g++
LINK = g++
INCLUDES = `pkg-config --cflags glib-2.0 gtk+-2.0`
LIBS = `pkg-config --libs glib-2.0 gtk+-2.0`
CFLAGS = -std=c++11 -O2 -Wall -g

all: $(NAME).o
	$(LINK) $(CFLAGS) $(INCLUDES) -o $(NAME) $(NAME).o $(LIBS)
	

%.o: %.cpp
	$(CXX) $(CFLAGS) $(INCLUDES) -c $< $(NAME).cpp $(LIBS)

clean:
	rm $(NAME).o $(NAME)
