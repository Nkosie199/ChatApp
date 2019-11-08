JFLAGS = -g
JC = javac

.SUFFIXES: .java .class

java.class:
	$(JC) $(JFLAGS) *.java

CLASSES = ChatApp.java \
	Server.java \
	Client.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class

