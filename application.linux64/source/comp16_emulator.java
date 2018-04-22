import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import static javax.swing.JOptionPane.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class comp16_emulator extends PApplet {

//Note: chars are used as unsigned 16 bit values

CPU cpu;

boolean RUN_CPU = false;

//Number of cycles to run between updating output
int CYCLES_PER_FRAME = 1000000;

int TIME_START = 0;
float SPEED_MHZ = 0;

//Path to file to load
String FILE_PATH = "";
//Fonts
PFont ctrl_font;
PFont c16font;

class REGS {
  static final int A=0, B=1, RES=2, PC=3, MAR=4, MDR=5, CND=6, BP=7, SP=8, CR=9, AX=10, BX=11, CX=12, DX=13, EX=14, FX=15;
}
class ALU_OPS {
  static final int ADD=0, SUB=1, MUL=2, NEG=3, BOR=4, BAND=5, NOT=6, AND=7, RSHIFT=8, LSHIFT=9, EQUAL=10, GREATER=11, GREATER_EQUAL=12, OR=13, BXOR=14;
}

class INSTR {
  static final int NOP=0, MOV=1, JMP=2, JPC=3, PRA=4, PRB=5, LOD=6, STR=7, PSH=8, POP=9, SRT=10, RET=11, OUT=12, IN=13;
}

//Number of clocks for each instruction - used to find clock speed
static final int[] INSTR_CLKS =  {2, 2, 3, 3, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2};

class CPU {
  char[] ram = new char[65536];
  char[] regs = new char[16];
  boolean[] we = new boolean[16];
  char port_io[] = new char[256];
  boolean[] port_we = new boolean[256];
  byte alu_op = 0;
  //Current Instruction
  char instr = 0;
  //Number of clock cycles run
  int clks = 0;
  
  //IO vars
  //GFX text
  char[] gfx_txt_mem = new char[1000];
  //PS2 keyboard Interface
  char[] key_mem = new char[256];
  char key_write_pos = 0;
  char key_read_pos = 0;
  
  public void run_alu(){
    char a = this.regs[REGS.A];
    char b = this.regs[REGS.B];
    switch(alu_op){
      case ALU_OPS.ADD:
        this.regs[REGS.RES] = (char)(a + b);
        break;
      case ALU_OPS.SUB:
        this.regs[REGS.RES] = (char)(a - b);
        break;
      case ALU_OPS.MUL:
        this.regs[REGS.RES] = (char)(a * b);
        break;
      case ALU_OPS.NEG:
        this.regs[REGS.RES] = (char)(~ a);
        break;
      case ALU_OPS.BOR:
        this.regs[REGS.RES] = (char)(a | b);
        break;
      case ALU_OPS.BAND:
        this.regs[REGS.RES] = (char)(a & b);
        break;
      case ALU_OPS.NOT:
        this.regs[REGS.RES] = (char)((a == 0) ? 1:0);
        break;
      case ALU_OPS.AND:
        this.regs[REGS.RES] = (char)( ((a!=0?true:false) && (b!=0?true:false))?1:0);
        break;
      case ALU_OPS.RSHIFT:
        this.regs[REGS.RES] = (char)(a >> b);
        break;
      case ALU_OPS.LSHIFT:
        this.regs[REGS.RES] = (char)(a << b);
        break;
      case ALU_OPS.EQUAL:
        this.regs[REGS.RES] = (char)((a == b)?1:0);
        break;
      case ALU_OPS.GREATER:
        this.regs[REGS.RES] = (char)((a > b)?1:0);
        break;
      case ALU_OPS.GREATER_EQUAL:
        this.regs[REGS.RES] = (char)((a >= b)?1:0);
        break;
      case ALU_OPS.OR:
        this.regs[REGS.RES] = (char)( ((a!=0?true:false) || (b!=0?true:false))?1:0);
        break;
      case ALU_OPS.BXOR:
        this.regs[REGS.RES] = (char)(a ^ b);
        break;
      default:
        this.regs[REGS.RES] = a;
      
    }
  }
  
  public void clear_we(){
    for(int i = 0; i < 16; i++){
       this.we[i] = false; 
    }
  }
  
  public void clear_port_we(){
     for(int i = 0; i < 256; i++){
        this.port_we[i] = false; 
     }
  }
  
  public void clear_regs(){
    for(int i = 0; i < 16; i++){
      this.regs[i] = (char)0;  
    }
  }
  
  public void clear_ram(){
    for(int i = 0; i < 65536; i++){
       this.ram[i] = (char)0; 
    }
  }
  
  public void clear_port_io(){
     for(int i = 0; i < 256; i++){
        this.port_io[i] = (char)0; 
     }
  }
  
  public void clear_gfx_txt_mem(){
     for(int i = 0; i < 1000; i++){
        this.gfx_txt_mem[i] = 0; 
     }
  }
  
  //Run the alu, load MDR from ram, load the instr, increment pc, clear we
  public void instr_setup(){
    this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
    this.clear_we();
    this.clear_port_we();
    this.run_alu();
    this.instr = this.ram[this.regs[REGS.PC]];
    this.regs[REGS.PC] += 1;
  }
  
  public void run_instr(){
    char op = (char)((this.instr & 0xf000) >> 12);
    //add # of clks to clks
    this.clks += INSTR_CLKS[op];
    char src = (char)((this.instr & 0x0f00) >> 8);
    char dst = (char)((this.instr & 0x00f0) >> 4);
    byte alu_op = (byte)(this.instr & 0x000f);
    char val = (char)(this.instr & 0x00ff);
    char val12 = (char)(this.instr & 0x0fff);
    switch (op){
      case INSTR.NOP:
        break;
      case INSTR.MOV:
        this.regs[dst] = this.regs[src];
        this.we[dst] = true;
        this.alu_op = alu_op;
        break;
      case INSTR.JMP:
        this.regs[src] = (char)((this.regs[src] & 0xff00) + val);
        this.regs[REGS.PC] = this.regs[src];
        this.we[REGS.PC] = true;
        break;
      case INSTR.JPC:
        this.regs[src] = (char)((this.regs[src] & 0xff00) + val);
        if(this.regs[REGS.CND] != 0){
          this.regs[REGS.PC] = this.regs[src];
        }
        this.we[REGS.PC] = true;
        break;
      case INSTR.PRA:
        this.regs[src] = (char)((this.regs[src] & 0xff00) + val);
        this.we[src] = true;
        break;
      case INSTR.PRB:
        this.regs[src] = (char)((val << 8) + (this.regs[src] & 0x00ff));
        this.we[src] = true;
        break;
      case INSTR.LOD:
        this.regs[REGS.MAR] = (char)((this.regs[REGS.MAR] & 0xff00) + val);
        this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
        this.regs[src] = this.regs[REGS.MDR];
        this.we[src] = true;
        break;
      case INSTR.STR:
        this.regs[REGS.MAR] = (char)((this.regs[REGS.MAR] & 0xff00) + val);
        this.regs[REGS.MDR] = this.regs[src];
        this.we[REGS.MDR] = true;
        break;
      case INSTR.PSH:
        this.regs[REGS.MAR] = this.regs[src];
        this.regs[REGS.MDR] = this.regs[dst];
        this.we[REGS.MDR] = true;
        this.regs[src] -= 1;
        break;
      case INSTR.POP:
        this.regs[REGS.MAR] = (char)(this.regs[src] + 1);
        this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
        this.regs[dst] = this.regs[REGS.MDR];
        this.regs[src] += 1;
        this.we[dst] = true;
        break;
      case INSTR.SRT:
        this.regs[src] = (char)((this.regs[src] & 0xff00)+val);
        this.regs[REGS.MAR] = this.regs[REGS.SP];
        this.regs[REGS.MDR] = this.regs[REGS.PC];
        this.regs[REGS.SP] -= 1;
        this.regs[REGS.PC] = this.regs[src];
        this.we[REGS.MDR] = true;
        this.we[REGS.PC] = true;
        break;
      case INSTR.RET:
        this.regs[REGS.MAR] = (char)(this.regs[REGS.SP] + 1);
        this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
        this.regs[REGS.PC] = this.regs[REGS.MDR];
        this.regs[REGS.SP] += 1 + val12;
        this.we[REGS.PC] = true;
        break;
      case INSTR.OUT:
        this.port_io[val] = this.regs[src];
        this.port_we[val] = true;
        break;
      case INSTR.IN:
        this.regs[src] = this.port_io[val];
        this.we[src] = true;
        break;
      default:
        break;
    }
  }
  
  public void handle_txt_mem(){
    //Write or read from gfx txt mem
    if(this.port_io[5] < 1000){
      if(this.port_we[6]){
        this.gfx_txt_mem[this.port_io[5]] = this.port_io[6];
      }
      else{
        this.port_io[6] = this.gfx_txt_mem[this.port_io[5]];
      }
    }
  }
  
  public void handle_key(){
     //Set key in waiting port to number of keys waiting
     this.port_io[8] = (char)(this.key_write_pos - this.key_read_pos);
     //set key data port
     this.port_io[7] = this.key_mem[this.key_read_pos];
     //If key next toggled, increment key read pos
     if(this.port_we[7]){
        this.key_read_pos++;
        this.key_read_pos %=256;
     }
  }
  
  //Write data to ram if mdr was written to
  public void instr_end(){
    if(this.we[REGS.MDR]){
      this.ram[this.regs[REGS.MAR]] = this.regs[REGS.MDR];  
    }
    this.handle_txt_mem();
    this.handle_key();
  }
  //Run one instr cycle
  public void run_instr_cycle(){
    this.instr_setup();
    this.run_instr();
    this.instr_end();
  }
  
  public void load_mem_from_file(String filename){
    byte[] data = loadBytes(filename);
    int addrs = 0;
    for(int i = 1; i < data.length; i+=2){
      this.ram[addrs] = (char)(((data[i-1]&0xFF)<<8) + (data[i]&0xFF));
      addrs++;
    }
  }
  
  public void reset(){
    this.clear_ram();
    this.clear_regs();
    this.clear_port_io();
    this.clear_we();
    this.clear_port_we();
    this.clear_gfx_txt_mem();
    this.key_write_pos = 0;
    this.key_read_pos = 0;
  }
  
  public void load_mem(){
    this.reset();
    selectInput("Select a Comp16 binary to load", "binSelected");
  }
  
}

public void binSelected(File selection) {
  if (selection == null) {
    FILE_PATH = "NULL";
  } else {
    FILE_PATH = selection.getAbsolutePath();
    cpu.load_mem_from_file(FILE_PATH);
  }
}

public void keyPressed(){
  addKey(false); 
}
public void keyReleased(){
   addKey(true); 
}

public void addKey(boolean release){
   //Convert to lowercase
  if(keyCode>=65&&keyCode<90){
    keyCode += 32;  
  }
  char code = (char)keyCode;
  //If it is a released key, add release code
  if(release){
  code += 0x0100;
  }
  cpu.key_mem[cpu.key_write_pos] = code;
  cpu.key_write_pos++; 
  cpu.key_write_pos %= 256;
}

public void drawControls(){
  textFont(ctrl_font);
  fill(255);
  rect(0,200,320,60);
  fill(200);
  rect(115, 230, 2, 27);
  fill(150);
  rect(5, 205, 20, 20);
  rect(30, 205, 20, 20);
  rect(55, 205, 110, 20);
  rect(170, 205, 45, 20);
  rect(220, 205, 20, 20);
  rect(245, 205, 70, 20);
  fill(0);
  text("Load Binary File", 60, 219);
  text("Reset", 177, 219);
  text("Set Num", 265, 220);
  text("Status: " + (RUN_CPU?"Running":"Stoped"), 5, 242);
  text("Clock: " + String.format("%.02f", SPEED_MHZ)+ " Mhz", 5, 257);
  text("A " + hex(cpu.regs[0]) + " RES " + hex(cpu.regs[2]) + " MAR " + hex(cpu.regs[4]) + " CND " + hex(cpu.regs[6]), 120, 242);
  text("B " + hex(cpu.regs[1]) + "  PC " + hex(cpu.regs[3]) + " MDR " + hex(cpu.regs[5]) + "  BP " + hex(cpu.regs[7]), 120, 257);
  fill(0,255,0);
  triangle(9, 209, 9, 221, 21, 215);
  fill(255,0,0);
  rect(34,209,12,12);
  fill(0,0,255);
  triangle(9+215, 209, 9+215, 221, 21+215, 215);
  rect(19+215, 209, 2, 12);
  
  triangle(9+240, 209, 9+240, 221, 21+240, 215);
  rect(19+240, 209, 2, 12);
}

public void drawScreen(){
  textFont(c16font);
  fill(0);
  rect(0,0,320,200);
  for(int y = 0; y < 25; y++){
     for(int x = 0; x < 40; x++){
       char data = cpu.gfx_txt_mem[x+y*40];
       char char_to_print = (char)(data&0xff);
       char col = (char)((data&0xff00)>>8);
       if(col == 0){
         col = 255;
       }
       fill(col & 0xe0, (col&0x1c)<<3, (col&0x3)<<6);
       
       //Block - Not in charset tty
       if(char_to_print == 0xdb){
         rect(x*8,y*8,8,8);  
       }
       else{
         text(char_to_print,x*8,(y+1)*8);
       }
     }
  }
}

//Check if mouse is in box
public boolean mbox(int x, int y, int w, int h){
  if(mouseX > x && mouseX < x+w && mouseY > y && mouseY < y+h){
    fill(100);
    rect(x,y,w,h);
    return true;
  }
  return false;
}

public void mouseClicked(){
  //Run button
  if(mbox(5, 205, 20, 20)){
    RUN_CPU = true;
    CYCLES_PER_FRAME = 1000000;
  }
  //Stop button
  if(mbox(30, 205, 20, 20)){
    RUN_CPU = false;
  }
  //Load Binary File button
  if(mbox(55, 205, 110, 20)){
    cpu.load_mem();
  }
  //Reset button
  if(mbox(170, 205, 45, 20)){
    cpu.reset(); 
    if(FILE_PATH != "" && FILE_PATH != "NULL"){
      cpu.load_mem_from_file(FILE_PATH); 
    }
  }
  //Step one cycle button
  if(mbox(220, 205, 20, 20)){
    RUN_CPU = true;
    CYCLES_PER_FRAME = 1;
  }
  //Step 1000 cycles button
  if(mbox(245, 205, 70, 20)){
    final String cycles = showInputDialog("Enter Cycles Per Frame");
    if(cycles != null){
      RUN_CPU = true;
      CYCLES_PER_FRAME = PApplet.parseInt(cycles);
    }
  }
}

public void setup(){
  cpu = new CPU();
  ctrl_font = createFont("Courier", 10);
  c16font = createFont("PxPlus_IBM_CGAthin.ttf", 8);
  noStroke();
  
  background(0);
  drawControls();
  if (args != null) {
    FILE_PATH = args[0];
    cpu.load_mem_from_file(FILE_PATH);
  }
}

public void draw(){
  if(RUN_CPU){
    TIME_START = millis();
    for(int i = 0; i < CYCLES_PER_FRAME; i++){
    cpu.run_instr_cycle();
    }
    SPEED_MHZ = ((float)(cpu.clks))/(millis()-TIME_START);
    SPEED_MHZ /= 1000.0f;
    cpu.clks = 0;
  }
  drawControls();
  drawScreen();
}
  public void settings() {  size(320, 260); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "comp16_emulator" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
