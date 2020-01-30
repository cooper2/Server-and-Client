# Authors:
# @Cooper
# @Saxon
# Version 1.2 29/05/2016

import socket
import ssl
import tarfile
import struct
import io
import threading
import optparse
import os
import sys
import time
import shlex
import OpenSSL
import array

	#Refereneces:
	#File directory pathing and help from: http://stackoverflow.com/questions/8024248/telling-python-to-save-a-txt-file-to-a-certain-directory-on-windows-and-mac
	#Parser info:  https://docs.python.org/2/library/optparse.html

	#Variables
#TODO docroot = 

	#Connection Variables
HOST = "localhost"
PORT = 7777
ADDR = (HOST,PORT)

	#Hard-coded Signature used for testing vouch method.
cert = "73 61 0D 91 F9 B7 B5 4D E1 77 65 03 90 B5 B7 67 3C FE 5E AD 08 0E F1 E5 5E 2C C5 6C F4 95 54 3B F5 F5 7F 99 1B 06 1F 57 50 38 29 EF CA 73 A0 F0 22 46 0A CE C7 92 2D 4D E2 2C E1 7F 6E 12 F2 6B 13 42 65 2D 64 DA A5 74 A4 A4 C6 00 85 CF 0F 01 64 E6 51 D2 FE 36 92 C1 31 2F 3B F8 81 5C E3 2C 41 2B 7A 0B E3 3F FD 7D 10 F4 CA 4D 35 7E 42 BA B9 A1 0F 4B 4F F0 9E 3D 12 AD 97 91 8E 97 82 B1 4C CD 48 5B 54 DF 71 04 E1 45 14 D6 DF FA 24 28 72 DA 42 EB 85 D0 50 21 C7 7A C1 6A 07 87 98 CE E6 D8 A3 46 61 32 3E CF 62 DC 9A 6D C8 E3 96 06 0D 89 56 3D 56 D0 D0 04 02 92 99 53 5E C3 E4 97 E8 3D 18 4C 35 C6 46 F0 80 0C B1 8A 21 37 96 43 29 36 7F 6F 1E DD 67 FE 8F 64 4A D8 87 11 B8 46 FE 05 BB 6D 53 6D 61 72 B3 30 21 74 F9 A4 6A C9 99 77 E7 55 8A BC D8 B7 E7 1E 78 A8 DB 1D 31 8F"
SIG = bytearray()
SIG.extend(cert)


	#Establish a SSL Socket Connection with a server specified using Connection variables (HOST, PORT).	
def sock_connection():
	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)			
			#Create a socket object in sock. AF_INET and SOCK_STREAM represent the address and protocol families used for socket(). (IPv4)
	sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
			#Can we test out this line? ^ I don't actually think it's necessary.
	ssl_sock = ssl.wrap_socket(sock)
			#This is an SSL wrapper for socket objects. Returned socket is tied to context, settings and certificates of original object.
	try:
		ssl_sock.connect(ADDR)
			#Connects the ssl_sock object to the address. 
		cipher = ssl_sock.cipher()
			#Cipher is never used, but provides the ciphering details of the connection/socket if you print them after.
	except socket.error as e:
		print('   Socket Connection Error: Could not establish connection', e)
		exit(1)
			#Throws an error if socket fails to connect
	return ssl_sock	#method returns the ssl_sock object, a socket connection wrapped with ssl.


	#Save a file downloaded from the server. The downloads file is specified in Variables.
def saveFile(filename, header):
	cwd = os.getcwd()
			#Get the current working directory.
	print("   This is the directory that saveFile uses: " + cwd)
	file = open(os.path.join(cwd + "/files", filename), "w") #TODO 
			#Creates a file object in the specified directory ^ that creates/overwrites the title of the file. "Text.txt"
	file.write(header)		  
			#Writes all the data from header into the file
			#And then closes the file.
	file.close()		
			

	

	#Returns the bytes from a file within the cwd, returns an error if file does not exist, overwrites if it does exist.
	#TODO Print 'Certificate to be uploaded to the server does not exist' for -u command.
def get_bytes_from_file(filename):
	cwd = os.getcwd()
		#Gets the current working directory.
	filenameValid = False
		#Declaring a variable to be False.
	os.chdir(cwd + '/files') #TODO
		#Changes the current working directory to files.
	cwd = os.getcwd()
		#Gets the current working directory
	files = os.listdir(cwd)
		#Creates a list called files containing all the names of all the files in 'files'.
	fileName = filename.split("/") 
		#Splits the argument filename into multiple strings, seperated by '/'.
	length = len(fileName)
		#Gets the number of strings in the variable fileName.
	realFile = fileName[length-1]
		#Using this length, returns the last string of fileName and stores it in realFile.
		#This way we have the name of the file we are loooking for.
	for stuff in files:
		if realFile == stuff:
			filenameValid = True
			#Creates a loop, where the directory is searched for the file name we are looking for,
			#by comparing the name of the file we want against every other file in the directory.
			#If the file we are looking for is equal to the title of a file stored within files, True is returned.
	if filenameValid is False:
		print("   The file to be added to the server does not exist: '%s'." % realFile)
			#This way if the file is never found, the user is told that the file doesn't exist.
		exit(1)
	else:
			#But if it is True, the file does exist!
		return open(filename, "rb") # REVISION removed .read() from end	(important later)
			#Then the contents of the file desired is returned in byte form.
	

	#Add_file adds or replaces a file on the server.
	#TODO Error methods incase the server failed to save the file.
	#TODO 
def add_file(filename0, filename1):
	cwd = os.getcwd()
	data = get_bytes_from_file(os.path.join(cwd + '/files',filename1))
			#Calls the method above to store the bytes from a file into the variable 'data'.
	print("   Sending from the following directory: " + cwd + "/files")
	sock = sock_connection()
			#creates an ssl wrapped socket and stores it in the variable sock.
	sock.send("-a" + " " + filename1 + " \n") # REVISION removed > symbol and data variable

        # REVISION added buffering to file sending (otherwise data not received properly)
        buffer = data.read(1024) #important that .read() was removed from get_bytes_from_file
        while ( buffer ):                       # while you can read anything from the data file
                sock.send( buffer )             # put data file bits in buffer and send them
                buffer = data.read(1024)        # get the next data bits for the buffer
        data.close()                            # close the open data file when finished
        
	
	print("   Sent '%s' to Server." % filename1)
	adding = "-a"
			#Since -a and -u utilise the same method, we create an if/else statement to discern which it is,
			#so that the print out messages can notify the user properly in terms of adding-files/uploading-certs.
	if adding == filename0:
		header = recv_timeout(sock)
			#So recv_timeout just listens to the sock to hear the data coming in.
		if header == None:
			print("   Server failed to send ACK. Unknown if file was successfully saved by the Server.")
				#And if no data comes in then it releases an error.
		else:
			print("   File '%s' successfully added to the Server." % filename1)
				#And our server is set to reply with anything if it completes the job correctly.
				#Therefore when our client receives anything at all through listen, it knows the job is successful.
			sock.close()
	else:
		header = recv_timeout(sock)
				#same deal here, except this is for certificates.
		if header == None:
			print("   Server failed to send ACK. Unknown if file was successfully saved by the Server.")
		else:
			print("   Certificate '%s' successfully added to the Server." % filename1)
			

	#fetch_file fetches an existing file from the server.
	#TODO Implement errors if file requested is a certificate (Do not allow).
	#TODO Implement errors if file fails to download.
def fetch_file(filename0, filename1):
	sock = sock_connection()
        sock.send(filename0 + " " + filename1 + "\n")   # REVISION removed > symbol
			#Sends bytes to the server with the '-f', the 'filename' and a new space character so the server can readLine.
	data = recv_timeout(sock)
			#Then it waits for server to send back data, and stores whatever it hears inside of data.
	if data is None:
		print("   This is not valid data.")
			#If the data receives nothing, then ^ an error occurs.
			#Otherwise the data is saves into a file with the same name as the filename1 argument, which is the file
			#argument to the -f command the user inputted.
	saveFile(filename1, data)
	sock.close()
	print("   Successfully acquired requested file: '%s'" % filename1)
	
	
	
	#YO COOPS DOG! I was wondering if you could annotate this part as im not exactly sure whats going on precisely,
	#and the API is confusing as hell for Python cause they never really explain stuff. Remove when done.
	
	#recv_timeout is used to acquire the incoming data from the server.
	#The timeout method is employed as an error checking procedure.
	#TODO Implement user messages if recv_timeout does not receive any packets while listening.
def recv_timeout(sock, timeout=0.1):
        sock.setblocking(0)
        total_data=[];
        data='';
        begin = time.time()
        while 1:
                #if you received some data, then break after timeout
                if total_data and time.time()-begin > timeout:
                        break

                #if you received no data at all, wait again for twice as long
                elif time.time()-begin > timeout*2:
                        break

                #recv something
                try:
			data = sock.recv(8192) 
                	if data:
                                total_data.append(data)
                                begin = time.time()
                        else:
                                time.sleep(0.1)
                except:
                        pass
        return ''.join(total_data)


	#TODO Implement formatting of received data.
def list_files():
	sock = sock_connection()
	sock.send("-l" + "\n")  # REVISION removed > symbol
			#Just sends the '-l' argument for the server to respond to.
	listFiles = recv_timeout(sock)
			#Then listens to receive data and stotes it within listFiles. The data received is a list of all the files.
	print("The files on the server are: " + listFiles) # REVISION added print statement
			#Prints them all.
	if listFiles is None:
		print("   Data received is not valid data.")
			#But if it's none then the user is provided with an error message because something went wrong server side.
	#TODO - This is where you format the whole list file stuff


	#TODO - Find way to send signature of certificates to server without cryptography package.
	#Currently hardcoding the signature to test vouch for the server.
def make_signature(pathToCert, certName):
	print('   None - no make_signature')	

	
	#TODO Implement method
def host_addr():
	print('   None - no host_addr()')
	

	#TODO 
def involve_name():
	#Do after make_sig, retrieve name of cert file.
	print('   None - no involve names')


	#TODO Make_signature comes first. Hard-coded for testing on server.
def file_vouch(fV, fileName, certificate):
#	make_signature()
	sock = sock_connection()
	sock.send("-v" + " " + "test.txt" + " " + "domain.crt" + " " + SIG + ">\n")
			#This was hardcoded to use as testing for the server side code.

	#TODO Last.
def circ_len():
	print('   None - no circ_len')



	#The options check for the argument parser. See below for more details.
	#TODO include another option -y for multiple arguments in a single line.	
	
	#Easiest way to describe this bit is it just checks whether or not the arguments are actually used
	#when the User inputs something into the command line. If they do, the letter is returned 'True'.
def checks(options):
	a, c, f, h, l, n, u, v = False, False, False, False, False, False, False, False 

	if options.add_file_path is not None:
		a = True
	if options.fetch_file_path is not None:
		f = True
	if options.circumference is not None:
		c = True
	if options.address is not None:
		h = True
	if options.list_files is not None:
		l = True
	if options.involve_name is not None:
		n = True
	if options.cert_file_path is not None:
		u = True
	if options.vouch_args is not None:
		v = True	
	return a, c, f, h, l ,n, u, v

	#The main method parses arguments and uses method calls to execute each scenario.
	#h has inbuilt parse argument 'help', -z is used instead.
	

	#TODO Input variable -y
def main():	
	parser = optparse.OptionParser()
	#Adds all the options to the server, it's pretty straight forward though right?
	#If you guys want an added explanation i can do it in person, or over text just lemme know.
	#But basicaly parser.add_option adds another command prompt to the enterable commands, and the rest
	#of the arguments specify the 'rules' around it like the no. of args it can take etc. etc.
	UserInput = raw_input("What would you like to do to the Server? The -h command provides valid arguments: ")
	parser.add_option('-a','--add',dest='add_file_path',help = "Add or replace a file on the oldtrusty server.", default = None, nargs=1)
	parser.add_option('-c','--circumference', dest='circumference',help ="Provide the required circumference (length) of a circle of trust.", default = None, nargs=1)
	parser.add_option('-f','--fetch', dest='fetch_file_path', help ="Fetch an existing file from the oldtrusty server.", default = None, nargs=1)
	parser.add_option('-z','--address', dest='address', help = "Provide the remote address hosting the oldtrusty server", default = None, nargs=1)
	parser.add_option('-l','--list', dest='list_files', help = "List all stored files and how they are protected.", default = None, nargs=0)
	parser.add_option('-n','--name', dest='involve_name', help = "Require a circle of trust to involve the named person (i.e. their certificate).", default = None, nargs=1)
	parser.add_option('-u','--upload', dest='cert_file_path', help = "Upload a certificate to the oldtrusty server.", default = None, nargs=1)
	parser.add_option('-v','--vouch', dest='vouch_args', help = "Vouch for the authenticity of an existing file in the oldtrusty server using the indicated certificate.",  default = None, nargs=2)
	(options, arguments) = parser.parse_args(shlex.split(UserInput))
	a, c, f, h, l, n, u, v  = checks(options)


		#Add or replace a file
		#TODO Implement an error code when the client never receives a NACK (self-made) from the server.
	if a:
		#kinda like saying 'if a is True', which is where the check(options) method comes in handy!
		filename = UserInput.split(" ")
		add_file(filename[0], filename[1])


		#Provide the required circumference (length) of a circle of trust.
	elif c:
		circ_length = UserInput.split(" ")
		circ_len(circ_length[0], circ_length[1])

		
		#TODO implement a code that does not let you download certs.
		#TODO Implement it Server side instead for extra protection if you have enough time. Client side is unsafe.
	elif f:
		filename = UserInput.split(" ")
		fetch_file(filename[0], filename[1])
		

		#TODO This whole function.
		#This currently grabs the second argument of -z and splits it into Host and Port.
	elif h: 
		new = options.address.split(":")
			#Splits the address at the ':', so new0 (the first string) is the host name
			#and the new1 (the second string) is the port number.
		print(new)
		HOST = str(new[0])
		PORT = new[1]
		print(HOST, PORT)
		sock_connection(HOST, PORT)
			#Then it connects to the new port but i dunno how to keep it there. Wouldn't it just reboot
			#on sequential commands? 
		

		#List all stored files and how they are protected.
	elif l:
		#TODO throw an exception if UserInput has args (client currently spits out error code)
		list_files()


		#Require a circle of trust to involve the named person.
	elif n:
		involveName = UserInput.split(" ")
		involve_name(involveName[0], involveName[1])

		
		#Upload a certificate to the server.
		#Uses the add_file command 
	elif u:
		certname = UserInput.split(" ")
		#The split command splits the string that was entered into user input at the " " characters.
		#That way we have every argument that we want from the command line seperately, on call.
		#It also means that for something like '-u Saxon.cert faljsdgh'fkjhaskjfhgsjkhfg'
		#everything after the filename is disregarded.
		add_file(certname[0],certname[1])


		#Vouch for the authenticity of an existing file in the server using the indicated certificate.
	elif v:
		vouching = UserInput.split(" ")
		file_vouch(vouching[0], vouching[1], vouching[2])


		#Invalid command statement.
	else:
		print('That was not a valid command. Use the -h command to print help if you need it.')


main()


