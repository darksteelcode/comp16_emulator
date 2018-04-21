all: install

install:
	sudo ln -sf `pwd`/application.linux64/comp16_emulator /usr/local/bin/c16emu
