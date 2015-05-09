package xyz.koral.internal;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.w3c.dom.Element;

import xyz.koral.Entry;



public class KeyIndices 
{
	static final long minKeyDistanceInBytes = 50000; // a key every 50.000 bytes
	
	public DenseLongVector indices = new DenseLongVector();
	public DenseLongVector byteOffsets = new DenseLongVector();

	/**
	 * create key indices from stream
	 */
	public KeyIndices(Element arrayElem, InputStream is)
	{
		try 
		{
			long sourceOffset = new Long(arrayElem.getAttribute(XmlDocument.sourceOffsetAtt));
			long noOfBytes = new Long(arrayElem.getAttribute(XmlDocument.noOfBytesAtt));
			PartInputStream pis = new PartInputStream(is, sourceOffset, noOfBytes);
			UTF8ByteCountReader reader = new UTF8ByteCountReader(pis, true);

			long lastKeyByteOffset = -minKeyDistanceInBytes;
			
			DenseCharVector indexPositionNumber = null;
			int readChars = -1;
			long currentIndex = 0;
			boolean checkForKey = true;
			while ((readChars = reader.read()) > 0)
			{
				for (int i=0; i<readChars; i++)
				{
					char c = reader.buf[i];
					
					if (indexPositionNumber != null)
					{
						if (c == ArrayStream.colPositionSep)
						{
							currentIndex = new Long(indexPositionNumber.toString());
							indexPositionNumber = null;
							checkForKey = true;
						}
						else
						{
							indexPositionNumber.add(c);
						}
					}
					else
					{
						if (c == ArrayStream.colSep)
						{
							currentIndex++;
							checkForKey = true;
						}
						else if (c == ArrayStream.colPositionSep)
						{
							indexPositionNumber = new DenseCharVector();
						}
						else if (checkForKey)
						{
							long offset = reader.offsets[i];
							if (offset - lastKeyByteOffset >= minKeyDistanceInBytes)
							{
								indices.add(currentIndex);
								byteOffsets.add(offset);
								
								lastKeyByteOffset = offset;
							}
							
							checkForKey = false;
						}
					}
				}
			}
			createXML(arrayElem);
		} 
		catch (IOException ex) 
		{
			throw new KoralError(ex);
		}
	}
	
	private void createXML(Element arrayElement)
	{
		StringWriter writer = new StringWriter();
		ArrayStreamWriter awriter = new ArrayStreamWriter(writer);
		
		for (int i=0; i<indices.size(); i++)
		{
			long index = i;
			long indexValue = indices.getL(i);
			long offset = byteOffsets.getL(i);
			awriter.accept(new Entry() 
			{
				public int strideSize() 
				{
					return 2;
				}
				
				public long pitchIndex() 
				{
					return 0;
				}
				
				public long index() 
				{
					return index;
				}
				
				public String getS(int strideIndex) 
				{
					return String.valueOf(strideIndex == 0 ? indexValue : offset);
				}
				
				public String[] getStrideS() { throw new NoSuchMethodError(); }
				
				public double getD(int strideIndex) { throw new NoSuchMethodError(); }
				public double[] getStrideD() { throw new NoSuchMethodError(); }

				public float getF(int strideIndex) { throw new NoSuchMethodError(); }
				public float[] getStrideF() { throw new NoSuchMethodError(); }
				public int getI(int strideIndex) { throw new NoSuchMethodError(); }
				public int[] getStrideI() { throw new NoSuchMethodError(); }
				public long getL(int strideIndex) { throw new NoSuchMethodError(); }
				public long[] getStrideL() { throw new NoSuchMethodError(); }
			});
		}
		
	    while (arrayElement.hasChildNodes())
	    	arrayElement.removeChild(arrayElement.getFirstChild());
		arrayElement.appendChild(arrayElement.getOwnerDocument().createTextNode(writer.toString()));
	}
	
	/**
	 * load indices from array 
	 */
	public KeyIndices(Element arrayElem)
	{
		ArrayStreamReader reader = new ArrayStreamReader(arrayElem.getTextContent());
		reader.forEach(entry -> {
			indices.add(new Long(entry.getS(0)));
			byteOffsets.add(new Long(entry.getS(1)));
		});
	}
}
