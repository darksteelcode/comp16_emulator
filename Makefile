all: cpu
cpu:	
	valac --pkg=gio-2.0 -o bin/cpu src/cpu.vala -X -O3	

clean:
	rm bin/*
