enum REGS {
A, B, RES, PC, MAR, MDR, CND, BP, SP, CR, AX, BX, CX, DX, EX, FX
}

enum ALU_OPS {
ADD, SUB, MUL, NEG, BOR, BAND, NOT, AND, RSHIFT, LSHIFT, EQUAL, GREATER, GREATER_EQUAL, OR, BXOR
}

enum INSTR {
NOP, MOV, JMP, JPC, PRA, PRB, LOD, STR, PSH, POP, SRT, RET, OUT, IN
}


class CPU : GLib.Object {
	int64[] INSTR_CLKS =  {2, 2, 3, 3, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2};
	//Main Memory
	public uint16[] ram = new uint16[65536];
	//Register File
	public uint16[] regs = new uint16[16];
	//Register File Write Enable
	public bool[] we = new bool[16];
	//Port IO Memory
	public uint16[] port_io = new uint16[256];
	//Port IO Write Enable
	public bool[] port_we = new bool[256];
	//ALU operation
	public uint8 alu_op = 0;
	//Current Instr
	public uint16 instr = 0;
	int64 time = GLib.get_real_time();

	//Run ALU operation on A and B regs, and sets RES to result
	void run_alu(){
		uint16 a = this.regs[REGS.A];
		uint16 b = this.regs[REGS.B];
		switch (alu_op){
			case ALU_OPS.ADD:
				this.regs[REGS.RES] = a + b;
				break;
			case ALU_OPS.SUB:
				this.regs[REGS.RES] = a - b;
				break;
			case ALU_OPS.MUL:
				this.regs[REGS.RES] = a * b;
				break;
			case ALU_OPS.NEG:
				this.regs[REGS.RES] = ~ a;
				break;
			case ALU_OPS.BOR:
				this.regs[REGS.RES] = a | b;
				break;
			case ALU_OPS.BAND:
				this.regs[REGS.RES] = a & b;
				break;
			case ALU_OPS.NOT:
				this.regs[REGS.RES] = (uint16)(! (bool)a);
				break;
			case ALU_OPS.AND:
				this.regs[REGS.RES] = (uint16)((bool)a && (bool)b);
				break;
			case ALU_OPS.RSHIFT:
				this.regs[REGS.RES] = a >> b;
				break;
			case ALU_OPS.LSHIFT:
				this.regs[REGS.RES] = a << b;
				break;
			case ALU_OPS.EQUAL:
				this.regs[REGS.RES] = (uint16)(a == b);
				break;
			case ALU_OPS.GREATER:
				this.regs[REGS.RES] = (uint16)(a > b);
				break;
			case ALU_OPS.GREATER_EQUAL:
				this.regs[REGS.RES] = (uint16)(a >= b);
				break;
			case ALU_OPS.OR:
				this.regs[REGS.RES] = (uint16)((bool)a || (bool)b);
				break;
			case ALU_OPS.BXOR:
				this.regs[REGS.RES] = a ^ b;
				break;
			default:
				this.regs[REGS.RES] = a;
				break;
		}
	}
	//clear we
	void clear_we(){
		for(int i = 0; i < 16; i++){
			this.we[i] = false;
		}
	}
	//clear port_we
	void clear_port_we(){
		for(int i = 0; i < 256; i++){
			this.port_we[i] = false;
		}
	}
	//run before each instr - run alu, increment pc, clear we and port_we, load instr from ram
	void instr_setup(){
		this.clear_we();
		this.clear_port_we();
		this.run_alu();
		this.instr = this.ram[this.regs[REGS.PC]];
		this.regs[REGS.PC] += 1;
	}
	//Returns the number of clk cycles the command takes.
	int64 run_instr(){
		uint16 op = (this.instr & 0xf000) >> 12;
		uint16 src = (this.instr & 0x0f00) >> 8;
		uint16 dst = (this.instr & 0x00f0) >> 4;
		uint8 alu_op = (uint8)(this.instr & 0x000f);
		uint16 val = this.instr & 0x00ff;
		uint16 val12 = this.instr & 0x0fff;
		switch (op){
			case INSTR.NOP:
				break;
			case INSTR.MOV:
				this.regs[dst] = this.regs[src];
				this.we[dst] = true;
				this.alu_op = alu_op;
				break;
			case INSTR.JMP:
				this.regs[src] = (this.regs[src] & 0xff00) + val;
				this.regs[REGS.PC] = this.regs[src];
				this.we[REGS.PC] = true;
				break;
			case INSTR.JPC:
				this.regs[src] = (this.regs[src] & 0xff00) + val;
				if((bool)this.regs[REGS.CND]){
					this.regs[REGS.PC] = this.regs[src];
				}
				this.we[REGS.PC] = true;
				break;
			case INSTR.PRA:
				this.regs[src] = (this.regs[src] & 0xff00) + val;
				this.we[src] = true;
				break;
			case INSTR.PRB:
				this.regs[src] = (val << 8) + (this.regs[src] & 0x00ff);
				this.we[src] = true;
				break;
			case INSTR.LOD:
				this.regs[REGS.MAR] = (this.regs[REGS.MAR] & 0xff00) + val;
				this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
				this.regs[src] = this.regs[REGS.MDR];
				this.we[src] = true;
				break;
			case INSTR.STR:
				this.regs[REGS.MAR] = (this.regs[REGS.MAR] & 0xff00) + val;
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
				this.regs[REGS.MAR] = this.regs[src] + 1;
				this.regs[REGS.MDR] = this.ram[this.regs[REGS.MAR]];
				this.regs[dst] = this.regs[REGS.MDR];
				this.regs[src] += 1;
				this.we[dst] = true;
				break;
			case INSTR.SRT:
				this.regs[src] = (this.regs[src] & 0xff00)+val;
				this.regs[REGS.MAR] = this.regs[REGS.SP];
				this.regs[REGS.MDR] = this.regs[REGS.PC];
				this.regs[REGS.SP] -= 1;
				this.regs[REGS.PC] = this.regs[src];
				this.we[REGS.MDR] = true;
				this.we[REGS.PC] = true;
				break;
			case INSTR.RET:
				this.regs[REGS.MAR] = this.regs[REGS.SP] + 1;
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
		return this.INSTR_CLKS[op];
	}
	//Write to mem if we on mdr is true
	void instr_end(){
		if(this.we[REGS.MDR]){
			this.ram[this.regs[REGS.MAR]] = this.regs[REGS.MAR];
		}
	}
	//Run n instr's - don't debug
	public void run(uint n){
		for(ulong i = 0; i < n; i++){
			this.instr_setup();
			this.run_instr();
			this.instr_end();
		}
	}

	public async void debug(int64 clks){
		print("\x1b[2J\x1b[H");
		int64 dur = GLib.get_real_time() - time;
		print("Speed: %f Mhz\nPort IO\n", ((double)clks)/((double)dur));
		for(uint16 p = 0; p < 256; p++){
			print("%04x ", this.port_io[p]);
			if(p%16==15){
				print("\n");
			}
		}
		this.time = GLib.get_real_time();
	}

	public async void start(bool debug=true){
		int64 clks = 0;
		while(true){
			this.instr_setup();
			clks += this.run_instr();
			this.instr_end();
			//yield;
			if(clks%1000000 == 0){
				if(debug){
					yield this.debug(clks);
				}
				clks = 0;
			}
		}
	}

	public void set_mem(uint16 addr, uint16 val){
		this.ram[addr] = val;
	}

	public bool load_from_file(string filename){
		try {
			uint8[] read;
        		FileUtils.get_data (filename, out read);
        		if(read.length % 2 != 0){
        			print("Warning: File %s is not proper length for a comp16 binary. It will not be loaded.\n", filename);
        			return false;
        		}
        		uint16 addr = 0;
        		for(uint32 i = 0; i < read.length; i++){
        			if(i%2==1){
        				uint16 data = (read[i-1] << 8) + read[i];
        				this.set_mem(addr, data);
        				addr++;
        			}
        		}
		} catch (FileError e) {
        		stderr.printf ("%s\n", e.message);
    		}
    		return false;
	}
}

void main(){
	var c = new CPU();
	c.load_from_file("prgm.bin");
	c.start();
	print("HELLO");
	var loop = new MainLoop();
	loop.run();
}

