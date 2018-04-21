# Comp16 Emulator
An emulator for [comp16](https://github.com/darksteelcode/comp16). It has complete graphics and keyboard eumlator, and is written in Processing (Java). Serial Port emulation is still being worked on.

## Installation
To download the emulator, the required tools to build software for comp16, and a sample of comp16 software, run the following:
```
cd ~
git clone https://github.com/darksteelcode/comp16_emulator
cd comp16_emulator
make install
cd ..
git clone https://github.com/darksteelcode/comp16_compilers
cd comp16_compilers
make install
cd ..
git clone https://github.com/darksteelcode/comp16_software
cd comp16_software
make
cd ..
```

## Usage
Run the emulator by typing the following in a terminal
```
c16emu
```
Use the start arrow and stop square to run and stop the emulator, use the "Load Binary File" button to choose a comp16 binary to run, and the "Reset" button to reset the currently loaded program.

Note: Once you choose a binary file to run, you still have to press the green start arrow to start the emulator.

To find the installed comp16_software binaries, look in ~/comp16_software
