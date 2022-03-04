/**
 * A JavaCard applet for managing the GlobalPIN
 * Must be installed with CVMManagement privilege e.g., using globalplatformpro:
 * > gp --install bin/SetupApplet.cap --privs CVMManagement
 * 
 * Chris Hicks, 2022
 */
package SetupApplet;

import javacard.framework.*;
import org.globalplatform.*;

public class SetupApplet extends Applet {

	// Setup code of CLA byte in the APDU header
    final static byte Setup_CLA =(byte) 0x80;

	// Applet codes of INS byte in the APDU header
	final static byte COUNT = (byte) 0x10;
	final static byte SET = (byte) 0x20;
    final static byte VERIFY = (byte) 0x30;

	// Signal that the PIN verification failed
    final static short PIN_VERIFICATION_FAILED = 0x6300;

	// Class variables
	static short le;
	static byte result;
	static CVM globalpin;
	static byte bLen;


	public SetupApplet(byte[] bArray, short bOffset, byte bLength) {
		byte aidLen = bArray[bOffset];
		// JCRE applet registration
		if (aidLen == (byte) 0) {
			register();
		}else {
			register(bArray, (short) (bOffset + 1), bArray[bOffset]);
		}
		globalpin = GPSystem.getCVM(GPSystem.CVM_GLOBAL_PIN);
	}

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		SetupApplet setupApp = new SetupApplet(bArray, bOffset, bLength);
	}

	/**
	 * Process incoming ADPUs to manage the card global PIN
	 */
	public void process(APDU apdu) {

		byte [] buffer = apdu.getBuffer();
		bLen = (byte)apdu.setIncomingAndReceive(); 

		if (apdu.isISOInterindustryCLA()) {
            if (buffer[ISO7816.OFFSET_INS] == (byte)(0xA4)) {
                return;
            } else {
                ISOException.throwIt (ISO7816.SW_CLA_NOT_SUPPORTED);
            }
        }

		if (buffer[ISO7816.OFFSET_CLA] != Setup_CLA)
        	ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);

		switch (buffer[ISO7816.OFFSET_INS]) {

			case COUNT:	// Fetch the number of PIN retries remaining

				le = apdu.setOutgoing();	// prepare to send to host
				if ( le < 2 )
					ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

        		apdu.setOutgoingLength((byte)1);
				buffer[0] = globalpin.getTriesRemaining();
				apdu.sendBytes((short)0, (short)1);
				
				return;

			case SET:	// Update the GlobalPIN

				buffer = apdu.getBuffer();	// Get remainder of buffer
				bLen = (byte)apdu.getIncomingLength(); 

				try{
					globalpin.resetState();

					// The CVM value must be put into a global array as the CVM is a global object.
					// The only global array is the APDU buffer so we send the PIN from the host.
					if(globalpin.update(buffer , (short) ISO7816.OFFSET_CDATA, (byte) buffer[ISO7816.OFFSET_LC], CVM.FORMAT_BCD)) {
						result = (byte)0x1;
					} else {
						result = (byte)0x0;
					}
				} catch (PINException e) {		
					result = (byte) 0xff;
				} catch (Exception e) {
					result = (byte) 0xee;
				}

				buffer[0] = result;

				le = apdu.setOutgoing();	// Tell host whether PIN update succeeded or failed.
				if ( le < 2 )
					ISOException.throwIt(ISO7816.SW_WRONG_LENGTH); // 6700

        		apdu.setOutgoingLength((byte)1);
				apdu.sendBytes( (short)0, (short)1 );

				return;

			case VERIFY:	// Verify the GlobalPIN

				buffer = apdu.getBuffer();	// Get remainder of buffer
				bLen = (byte)apdu.getIncomingLength(); 

				if(CVM.CVM_SUCCESS == globalpin.verify(buffer, (short) ISO7816.OFFSET_CDATA, 
																(byte) buffer[ISO7816.OFFSET_LC], CVM.FORMAT_BCD)) {
					result = (byte) 0x1;										
				} else {
					result = (byte) 0x0;
				}

				buffer[0] = result;

				le = apdu.setOutgoing();	// Tell host whether PIN verify succeeded or failed.
				if ( le < 2 )
					ISOException.throwIt(ISO7816.SW_WRONG_LENGTH); // 6700

        		apdu.setOutgoingLength((byte)1);
				apdu.sendBytes( (short)0, (short)1 );

				return;

			default:       
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		
		}
	}
  
	
}
