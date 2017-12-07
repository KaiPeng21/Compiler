LIB_ANTLR := lib/antlr.jar
ANTLR_SCRIPT := Micro.g4

all: team compiler

compiler:
	rm -rf build
	mkdir build
#	java -cp $(LIB_ANTLR) org.antlr.v4.Tool -o build $(ANTLR_SCRIPT)
	java org.antlr.v4.Tool -o build $(ANTLR_SCRIPT)
	rm -rf classes
	mkdir classes
#	javac -target 1.7 -source 1.7 -cp $(LIB_ANTLR) -d classes src/*.java build/*.java
	javac -target 1.7 -source 1.7 -d classes src/*.java build/*.java
clean:
	rm -rf classes build

team:
	@echo "Team: TooYoungTooSimple"
	@echo ""
	@echo "ChiaHua Peng"
	@echo "KaiPeng21"
	@echo ""
	@echo "Shaojiang Zhong"
	@echo "ShaojiangZhong"
	@echo ""

.PHONY: all team compiler clean
