/* Comp16 Demo Program - darksteelcode
 * This program
 */
label loop;
//Increment AX
mov AX A 0;
prb B 0;
pra B 1;
mov RES AX;
//Only change port 0 if AX<<8 is 0
mov AX A 9; /*Op 9 is <<*/
prb B 0;
pra B 8;
mov RES CND;
jpc FX loop;
prb B 0;
pra B 1;
in A 0;
out RES 0;
jmp FX loop;
