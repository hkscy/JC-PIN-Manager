# Client side tool for managing a JavaCard global PIN using SetupApplet.cap
# Install the applet and then run this script to set the globalPIN
#
# Chris Hicks, 2022
from smartcard.CardType import AnyCardType
from smartcard.CardRequest import CardRequest
from smartcard.CardConnectionObserver import ConsoleCardConnectionObserver
from smartcard.Exceptions import CardRequestTimeoutException
from smartcard.Exceptions import NoCardException
from smartcard.System import readers
from smartcard.util import toHexString
import sys

SELECT = [0x00, 0xA4, 0x04, 0x00, 0x08] # + AID to select
GET_RESPONSE = [0x00, 0xC0, 0x00, 0x00] # + n bytes to read
SETUPAPPLET_AID = [0x0A, 0x00, 0xC0, 0xFF, 0xEE, 0x00, 0x01, 0x00]
# Proprietary applet command(s)
PIN_ATTEMPTS_REMAIN = [0x80, 0x10, 0x00, 0x01, 0x00] # Get number of attempts remaining on global PIN
PIN_RESET =   [0x80, 0x20, 0x00, 0x01, 0x04] # Reset the global PIN to the FORMAT_BCD PIN following
DEFAULT_PIN = [0x30, 0x30, 0x30, 0x30] 
PIN_VERIFY = [0x80, 0x30, 0x00, 0x01, 0x04]	# Verify the global PIN to the FORMAT_BCD PIN following

PIN_OUTCOME_DICT = {1:'SUCCESS', 0:'FAILURE', 0xEE:'Unhandled Exception!', 0xFF: 'PIN Exception thrown by card!'}

for reader in readers():
	try:
		connection = reader.createConnection()
		connection.connect()
		print('Reader: ' + str(reader))
		print('Card ATR: ' + toHexString(connection.getATR()))

	except NoCardException:
		print(reader, 'no card inserted')

cardtype = AnyCardType()

try:
	cardrequest = CardRequest(timeout=10, cardType=cardtype)
	cardservice = cardrequest.waitforcard()

	# attach the console tracer
	observer = ConsoleCardConnectionObserver()
	cardservice.connection.addObserver(observer)

	cardservice.connection.connect()

	apdu = SELECT + SETUPAPPLET_AID
	response, sw1, sw2 = cardservice.connection.transmit(apdu)

	if sw1==0x90 and sw2==0x00:	# Applet is installed
		print('Setup applet selected.')
	else:
		print('SetupApplet not found, please install SetupApplet.cap before running this program.')
		exit()

	print('Attempting to reset global PIN to default/known value.')

	apdu = PIN_RESET + DEFAULT_PIN
	response, sw1, sw2 = cardservice.connection.transmit(apdu)

	if sw1==0x61:	# Command successful
		print('{} bytes available to be read from card.'.format(sw2))

		apdu =  GET_RESPONSE + [sw2]
		response, sw1, sw2 = cardservice.connection.transmit(apdu)

		if sw1==0x90 and sw2==0x00:	# Bytes read successfully
			print('Response from PIN reset operation: {}.'.format(PIN_OUTCOME_DICT[response[0]]))
			if response[0] > 1:
				exit()
		else:
			print('Error: unexpected response from card: {:02X}{:02X}'.format(sw1,sw2))
			exit()
	
	else:
		print('Unexpected response from card:  {:02X}{:02X}'.format(sw1,sw2))

	print('Verifying global PIN: {}'.format(''.join([hex(b)[2:][0] for b in DEFAULT_PIN])))

	apdu = PIN_VERIFY + DEFAULT_PIN
	response, sw1, sw2 = cardservice.connection.transmit(apdu)

	if sw1==0x61:	# Command successful
		print('{} bytes available to be read from card.'.format(sw2))

		apdu =  GET_RESPONSE + [sw2]
		response, sw1, sw2 = cardservice.connection.transmit(apdu)

		if sw1==0x90 and sw2==0x00:	# Bytes read successfully
			print('Response from PIN verify operation: {}.'.format(PIN_OUTCOME_DICT[response[0]]))
		else:
			print('Error: unexpected response from card: {:02X}{:02X}'.format(sw1,sw2))
			exit()
	
	else:
		print('Unexpected response from card:  {:02X}{:02X}'.format(sw1,sw2))

except CardRequestTimeoutException:
    print('time-out: no card inserted during last 10s') 