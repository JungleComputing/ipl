package ibis.ipl.impl.stacking.p2p.util;

public class Pair {
	private int prefix, digit;

	public Pair(int prefix, int digit) {
		this.prefix = prefix;
		this.digit = digit;
	}
	
	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(int prefix) {
		this.prefix = prefix;
	}

	/**
	 * @return the prefix
	 */
	public int getPrefix() {
		return prefix;
	}

	/**
	 * @param digit the digit to set
	 */
	public void setDigit(int digit) {
		this.digit = digit;
	}

	/**
	 * @return the digit
	 */
	public int getDigit() {
		return digit;
	}
}
