import RPi.GPIO as GPIO
import socket

# Set aliases for pins
PWMA = 13
AIN1 = 24
AIN2 = 23

STBY = 26

PWMB = 12
BIN1 = 20
BIN2 = 21

OFF = 0
ON = 1
#-----------------

GPIO.setmode(GPIO.BCM)
GPIO.setup(AIN1, GPIO.OUT)
GPIO.setup(AIN2, GPIO.OUT)
GPIO.setup(STBY, GPIO.OUT)
GPIO.setup(BIN1, GPIO.OUT)
GPIO.setup(BIN2, GPIO.OUT)

GPIO.setup(PWMA, GPIO.OUT)
GPIO.setup(PWMB, GPIO.OUT)
pwma = GPIO.PWM(PWMA, 100)
pwmb = GPIO.PWM(PWMB, 100)

# Initialize motor driver into stopped state
def on():
	log("power on")
	GPIO.output(STBY, ON)
	pwma.start(0)
	pwmb.start(0)
	stop()

# Stop and turn off motor driver
def off():
	log("power off")
	setSpeed(0)
	GPIO.output(AIN1, OFF)
	GPIO.output(AIN2, OFF)
	GPIO.output(BIN1, OFF)
	GPIO.output(BIN2, OFF)
	GPIO.output(STBY, OFF)

curspeed = 0
speeda = 1.0
speedb = 1.0

# Speed from 0-100, set to 0 for short-brake
# TODO short brake uses power, and stop() should be called if done
def setSpeed(speed):
	global curspeed
	global speeda
	global speedb

	curspeed = speed
	log("setting speed to (" + str(speed*speeda) + ", " + str(speed*speedb) + ")")
	pwma.ChangeDutyCycle(int(speed*speeda))
	pwmb.ChangeDutyCycle(int(speed*speedb))

def setSteer(steer):
	global curspeed
	global speeda
	global speedb

	steer = steer/100.0
	log("setting steer to " + str(steer))

	if(steer < 0.5):
		speeda = steer*2
		speedb = 1.0
	elif(steer > 0.5):
		speedb = (0.5 - (steer - 0.5))*2
		speeda = 1.0
	else:
		speeda = 1.0
		speedb = 1.0
	setSpeed(curspeed)

# Three states: Forward, Backward, Stop
# Forward and backward use power even at speed 0

# Backward state
def backward():
	log("backward state")
	#Clockwise
	GPIO.output(AIN1, ON)
	GPIO.output(AIN2, OFF)
	GPIO.output(BIN1, ON)
	GPIO.output(BIN2, OFF)

# Forward state
def forward():
	log("forward state")
	#Counter clockwise
	GPIO.output(AIN1, OFF)
	GPIO.output(AIN2, ON)
	GPIO.output(BIN1, OFF)
	GPIO.output(BIN2, ON)

# Stopped state, no power drain
def stop():
	log("stopped state")
	setSpeed(0)
	GPIO.output(AIN1, OFF)
	GPIO.output(AIN2, OFF)
	GPIO.output(BIN1, OFF)
	GPIO.output(BIN2, OFF)

# Handle an incoming data message
def handle(message):
	message = message.strip()
	if(message == ""):
		return
	messages = message.split(";")
	if(len(messages) > 1):
		for m in messages:
			handle(m)
	else:
		message = messages[0]

	print("handling " + message)
	
	unexpected = False

	if(message[:10] == "motorstate"):
		if(message[-7:] == "reverse"):
			backward()
		elif(message[-4] == "stop"):
			stop()
		elif(message[-7:] == "forward"):
			forward()
		else:
			unexpected = True
	elif(message[:10] == "motorspeed"):
		power = 0
		try:
			power = int(message[-3:])
		except ValueError:
			unexpected = True
		setSpeed(power)
	elif(message[:10] == "motorpower"):
		if(message[-4:] == "true"):
			on()
		elif(message[-5:] == "false"):
			off()
		else:
			unexpected = True
	elif(message[:5] == "steer"):
		steer = 50
		try:
			steer = int(message[-3:])
		except ValueError:
			unexpected = True
		setSteer(steer)
	else:
		unexpected = True

	if(unexpected):
		print("unexpected message " + message)
			
def log(string):
	print("LOG: " + string)

def startServer():
	s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	s.bind(('', 303))
	s.listen(1)
	# Loop each time a connection is made
	while True:
		print("Listening for connection")
		(connection, address) = s.accept()
		print("Accepted connection")
		# Loop for each communication with client
		while True:
			data = connection.recv(1024)
			handle(data)
			
startServer()
