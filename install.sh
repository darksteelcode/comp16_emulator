sudo ln -sf `pwd`/application.linux64/comp16_emulator /usr/local/bin/c16emu
cd ..
git clone https://github.com/darksteelcode/comp16_compilers
cd comp16_compilers
make install
cd ..
git clone https://github.com/darksteelcode/comp16_software
cd comp16_software
make
cd ..

