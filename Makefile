all: install

install:
	chmod +x install.sh
	./install.sh

clean:
	rm -rf application.*
