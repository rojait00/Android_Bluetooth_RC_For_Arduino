/*
 Name:		RemoteControl.ino
 Created:	1/6/2017 9:46:22 AM
 Author:	Rojait00
*/
#include <Servo.h>

///Input:
#define Bluetooth
//#define RF_Input

///Output:
//#define RF_Output
#define ServoOutput


#define Pin_ServoX 7
#define Pin_ServoY 8

#define DEBUG

#ifdef DEBUG
#define Serial_print() (Serial.print())
#define Serial_println() (Serial.println())
#else
#define Serial_print()
#define Serial_println()
#endif // DEBUG

#ifdef RF_Input
	#define receive() (receiveRF())
	#define setupInput() (setupRF_input())
#else
	#ifdef Bluetooth
		#define receive() (receiveBT())
		#define setupInput() (setupBT())
	#endif // Bluetooth
#endif // RF_input

#ifdef RF_Output
	#define setupOutput() (setupRF_output())
	#define writeValues() (sendRF())
#else 
	#ifdef ServoOutput
		#define setupOutput() (initServos())
		#define writeValues() (setServo())
	#endif // Servo
#endif // RF_output


/*############################## Begin of Code ###########################################*/

int x = 0;
int xOld = 0;
int y = 0;
int yOld = 0;
#ifdef ServoOutput
Servo xServo;
Servo yServo;
#endif // ServoOutput

void setup() {
	setupInput();
	setupOutput();
}

// the loop function runs over and over again until power down or reset
void loop() {
	String input = receive();
	if (input != "")
	{
		decodeInput(input);
		writeValues();
	}
}

void decodeInput(String input)
{
	for (int i = 0; i < input.length(); i++)
	{
		if (input.charAt(i)=='x'|| input.charAt(i) == 'X')
		{
			x = getVal(input, &i);
		}
		else if (input.charAt(i) == 'y' || input.charAt(i) == 'Y')
		{
			y = getVal(input, &i);
		}
	}
}

int getVal(String input, int* offset)
{
	String outStr = "";
	for (; *offset < input.length(); (*offset)++)
	{
		char c = input.charAt(*offset);
		if (isDigit(c))
		{
			outStr += c;
		}
		else
		{
			return outStr.toInt();
		}
	}
}

/*############################## RF Input ###########################################*/

String receiveRF()
{

}

void setupRF_input()
{

}


/*############################## BT Input ###########################################*/

String receiveBT()
{
	if (Serial.available())
		return Serial.readString();
	else
		return "";
}
void setupBT()
{
	Serial.begin(9600);
}

/*############################## RF Output ###########################################*/


void setupRF_output()
{

}
void sendRF()
{

}


/*############################## Servo Output ###########################################*/

void initServos()
{
	xServo.attach(Pin_ServoX);
	xServo.attach(Pin_ServoY);
}
void setServo()
{
	if (x != xOld)
	{
		xServo.write(x);
	}
	if (y != yOld)
	{
		yServo.write(y);
	}
}