package org.das2.datum;

import java.text.ParseException;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;

/** Represents a point in time, over thousands of years to nano-second resolution.
 * The Gegorian calendar is extended in both directions, which makes little sense
 * for dates prior to it's adoption.
 * 
 * @author ljg, eew, jbf, cwp
 */
public class CalendarTime implements Comparable<CalendarTime>{

	/** The time point's year number.
	 *  Note: that year 1 BC is represented year 0 in this field.
	 */
	protected int m_nYear;

	/** The time point's month of year, normalized range is 1 to 12 */
	protected int m_nMonth;

	/** The time point's day of month, normalized range is 1 up to 31
	 * depending on the month rValue.
	 */
	protected int m_nDom;

	// Cash the day of year calculation after a normalize.
	protected int m_nDoy;
	
	/** The time point's hour of day, normalized range is 0 to 23 */
	protected int m_nHour;

	/** The time point's minute of hour, normalized range is 0 to 59 */
	protected int m_nMinute;

	/** The time point's second of minute, normalized range is 0 to 59.
	 * Note that leap seconds are <b>not</b> handled by this class, though it
	 * wouldn't be hard to do so.
	 */
	protected int m_nSecond;

	/** The time point's nanosecond of second, normalized range is 0 to 999,999,999 */
	protected long m_nNanoSecond;


	////////////////////////////////////////////////////////////////////////////////////
	/** Empty constructor */

	public CalendarTime(){
		m_nYear = 1;
		m_nMonth = 1;
		m_nDom = 1;
		m_nDoy = 1;
		m_nHour = 0;
		m_nMinute = 0;
		m_nSecond = 0;
		m_nNanoSecond = 0;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Static method to create a calender time set to now */
	static public CalendarTime now(){

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));


		CalendarTime ct = new CalendarTime();
		ct.m_nYear = cal.get(Calendar.YEAR);
		ct.m_nMonth = cal.get(Calendar.MONTH) + 1;
		ct.m_nDom = cal.get(Calendar.DAY_OF_MONTH);
		ct.m_nDoy = cal.get(Calendar.DAY_OF_YEAR);
		ct.m_nHour = cal.get(Calendar.HOUR_OF_DAY);
		ct.m_nMinute = cal.get(Calendar.MINUTE);
		ct.m_nSecond = cal.get(Calendar.SECOND);
		ct.m_nNanoSecond = cal.get(Calendar.MILLISECOND) * 1000000L;

		return ct;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Tuple constructor
	 * @param lFields an array of integer fields.  Values are assumed to be in largest
	 *        time span to smallest time span.  The array may be null.  Up to 7 items will
	 *        be used from the array in the order: year, month, day, hour, min, sec,
	 *        nanosecond.
	 */
	public CalendarTime(int... lFields){
		m_nYear = 1;
		m_nMonth = 1;
		m_nDom = 1;
		m_nDoy = 1;
		m_nHour = 0;
		m_nMinute = 0;
		m_nSecond = 0;
		m_nNanoSecond = 0;

		if(lFields == null) return;
		if(lFields.length > 0) m_nYear = lFields[0];
		if(lFields.length > 1) m_nMonth = lFields[1];
		if(lFields.length > 2) m_nDom = lFields[2];
		if(lFields.length > 3) m_nHour = lFields[3];
		if(lFields.length > 4) m_nMinute = lFields[4];
		if(lFields.length > 5) m_nSecond = lFields[5];
		if(lFields.length > 6) m_nNanoSecond = lFields[6];

		normalize();
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Copy constructor */
	public CalendarTime(CalendarTime other){
		m_nYear = other.m_nYear;
		m_nMonth = other.m_nMonth;
		m_nDom = other.m_nDom;
		m_nDoy = other.m_nDoy;
		m_nHour = other.m_nHour;
		m_nMinute = other.m_nMinute;
		m_nSecond = other.m_nSecond;
		m_nNanoSecond = other.m_nNanoSecond;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Constructing a calendar time from a time string.
	 * Taken from Larry Granroth's Das1 lib.
	 */
	public CalendarTime(String s) throws ParseException{
		
		final int DATE = 0;
		final int YEAR = 1;
		final int MONTH = 2;
		final int DAY = 3;
		final int HOUR = 4;
		final int MINUTE = 5;
		final int SECOND = 6;

		final String DELIMITERS = " \t/-:,_;\r\n";
		final String PDSDELIMITERS = " \t/-T:,_;\r\n";

		final String[] months = {
			"january", "febuary", "march", "april", "may", "june",
			"july", "august", "september", "october", "november", "december"
		};
		final String[] mons = {
			"Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
		};

		// Starting values for this object, does not default to current year.
		m_nYear = 0;
		m_nMonth = 0;
		m_nDom = 0;
		m_nDoy = 0;
		m_nHour = 0;
		m_nMinute = 0;
		m_nSecond = 0;
		m_nNanoSecond = 0;

		String[] lToks = new String[10];
		
		int[] format = new int[7];

		int ptr;
		
		int hold;
		
		int tokIndex;

		/* handle ISO8601 time format */
		String delimiters = DELIMITERS;
		int iC;
		if((iC = s.indexOf((int) 'Z')) != -1){
			s = s.substring(0, iC);
		}
		int end_of_date = s.indexOf((int) 'T');
		if(end_of_date != -1){
			iC = end_of_date - 1;
			if(Character.isDigit(s.charAt(iC))){
				delimiters = PDSDELIMITERS;
			}
			else{
				end_of_date = -1;
			}
		}

		/* if not PDS then count out 3 non-space delimiters */
		int nTokens = 0;
		if(end_of_date == -1){
			nTokens = 0;
			int nLen  = s.length();
			for(int i = 0; i < nLen; i++){
				if((iC = (delimiters.substring(2)).indexOf(s.charAt(i))) != -1){
					nTokens++;
				}
				if(nTokens == 3){
					end_of_date = i;
					break;
				}
			}
		}

		/* tokenize the time string */
		StringTokenizer st = new StringTokenizer(s, delimiters);

		if(!st.hasMoreTokens())
			throw new java.text.ParseException("No tokens in '" + s + "'", 0);

		for(nTokens = 0; nTokens < 10 && st.hasMoreTokens(); nTokens++)
			lToks[nTokens] = st.nextToken();
		
		boolean[] lWant = new boolean[]{false, false, false, false, false, false, false};
		lWant[DATE] = lWant[YEAR] = lWant[MONTH] = lWant[DAY] = true;
		hold = 0;

		tokIndex = -1;

		// The big token parser loop, each iteration handles one token from the input string
		for(int i = 0; i < nTokens; i++){
			tokIndex = s.indexOf(lToks[i], tokIndex + 1);
			if((end_of_date != -1) && lWant[DATE] && tokIndex > end_of_date){
				lWant[DATE] = false;
				lWant[HOUR] = lWant[MINUTE] = lWant[SECOND] = true;
			}

			int nTokLen = lToks[i].length();
			double rValue;
			
			// skip 3-digit day-of-year values in parenthesis
			if ((nTokLen == 5) && lToks[i].startsWith("(") && lToks[i].endsWith(")")) {
				try{
					rValue = Double.parseDouble(lToks[i].substring(1, nTokLen-2));
				}
				catch(NumberFormatException e){
					throw new ParseException("Error in token '"+lToks[i]+"'", 1);
				}
				if ((rValue > 0) && (rValue < 367)) continue;
			}

			try{
				rValue = Double.parseDouble(lToks[i]);
			}
			catch(NumberFormatException e){
				if(nTokLen < 3 || !lWant[DATE]){
					throw new ParseException("Error at token '"+lToks[i]+"' in '"+s+"'", 0);
				}
				for(int j = 0; j < 12; j++){
					if(lToks[i].equalsIgnoreCase(months[j]) ||
						lToks[i].equalsIgnoreCase(mons[j]))   {
						m_nMonth = j + 1;
						lWant[MONTH] = false;
						if(hold > 0){
							if(m_nDom > 0)
								throw new ParseException("Ambiguous dates in token '" + lToks[i] +
									                      "' in '" + s + "'", 0);
							m_nDom = hold;
							hold = 0;
							lWant[DAY] = false;
						}
						break;
					}
				}
				if(lWant[MONTH])
					throw new ParseException("Error at token '"+lToks[i]+"' in '"+s+"'", 0);
				continue;
			}

			if(Math.IEEEremainder(rValue, 1.0) != 0.0){
				if(lWant[SECOND]){
					//Round normally to nearest nanosecond
					long nTmp = Math.round( rValue * 1.0e+9);
					m_nSecond = (int) ((long)nTmp / 1000000000L);
					m_nNanoSecond = (long)nTmp % 1000000000L;
					break;
				}
				else{
					throw new ParseException("Error at token '"+lToks[i]+"' in '"+s+"'", 0);
				}
			}

			int number = (int) rValue;
			if(number < 0){
				throw new ParseException("Error at token '"+lToks[i]+"' in '"+s+"'", 0);
			}

			if(lWant[DATE]){

				if(number == 0){
					throw new ParseException("m,d, or y can't be 0 in '" + s + "'", 0);
				}

				if(number >= 10000000 && lWant[YEAR]){ // %Y%m%d
					m_nYear = number / 10000;
					lWant[YEAR] = false;
					m_nMonth = number / 100 % 100;
					lWant[MONTH] = false;
					m_nDom = number % 100;
					m_nDoy = 0;
					lWant[DAY] = false;
				}
				else if(number >= 1000000 && lWant[YEAR]){ //%Y%j
					m_nYear = number / 1000;
					lWant[YEAR] = false;
					m_nDoy = number % 1000;
					m_nMonth = 0;
					lWant[MONTH] = false;
					lWant[DAY] = false;

				}
				else if(number > 31){

					if(lWant[YEAR]){
						m_nYear = number;
						if(m_nYear < 1000){
							m_nYear += 1900;
						}
						lWant[YEAR] = false;
					}
					else if(lWant[MONTH]){
						lWant[MONTH] = false;
						m_nMonth = 0;
						m_nDoy = number;
						lWant[DAY] = false;
					}
					else{
						throw new ParseException("Error at token '"+lToks[i]+"' in '"+s+"'", 0);
					}

				}
				else if(number > 12){

					if(lWant[DAY]){
						if(hold > 0){
							m_nMonth = hold;
							lWant[MONTH] = false;
						}
						if(nTokLen == 3){
							if(m_nMonth > 0){
								throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
							}
							m_nDoy = number;
							m_nDom = 0;
							lWant[MONTH] = false;
						}
						else{
							m_nDom = number;
						}
						lWant[DAY] = false;
					}
					else{
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}

				}
				else if(!lWant[MONTH]){

					if(m_nMonth > 0){
						m_nDom = number;
						m_nDoy = 0;
					}
					else{
						m_nDoy = number;
						m_nDom = 0;
					}
					lWant[DAY] = false;

				}
				else if(!lWant[DAY]){

					if(m_nDoy > 0){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					m_nMonth = number;
					lWant[MONTH] = false;

				}
				else if(!lWant[YEAR]){

					if(nTokLen == 3){
						if(m_nMonth > 0){
							throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
						}
						m_nDoy = number;
						m_nDom = 0;
						lWant[DAY] = false;
					}
					else{
						if(m_nDoy > 0){
							throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
						}
						m_nMonth = number;
						if(hold > 0){
							m_nDom = hold;
							lWant[DAY] = false;
						}
					}
					lWant[MONTH] = false;

				}
				else if(hold > 0){

					m_nMonth = hold;
					hold = 0;
					lWant[MONTH] = false;
					m_nDom = number;
					lWant[DAY] = false;

				}
				else{
					hold = number;
				}

				if(!(lWant[YEAR] || lWant[MONTH] || lWant[DAY])){
					lWant[DATE] = false;
					lWant[HOUR] = lWant[MINUTE] = lWant[SECOND] = true;
				}

			}
			else if(lWant[HOUR]){

				if(nTokLen == 4){
					hold = number / 100;
					// TODO: handle times like Jan-1-2001T24:00 --> Jan-2-2001T00:00,  for ease of modifying times
					if(hold > 23){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					m_nHour = hold;
					hold = number % 100;
					if(hold > 59){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					m_nMinute = hold;
					lWant[MINUTE] = false;
				}
				else{
					if(number > 23){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					m_nHour = number;
				}
				lWant[HOUR] = false;

			}
			else if(lWant[MINUTE]){
				// TODO: handle times like 0:90 --> 1:30,  for ease of modifying times
				if(number > 59){
					throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
				}
				m_nMinute = number;
				lWant[MINUTE] = false;

			}
			else if(lWant[SECOND]){

				if(number > 61){
					throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
				}
				m_nSecond = number;
				lWant[SECOND] = false;

			}
			else{
				throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
			}

		}
		// End of token parsing loop

		if(m_nMonth > 12){
			throw new ParseException("Month is greater than 12 in '" + s + "'", 0);
		}
		if(m_nMonth > 0 && m_nDom <= 0){
			m_nDom = 1;
		}

		int iLeap = ((m_nYear % 4) != 0 ? 0 : ((m_nYear % 100) > 0 ? 1 : ((m_nYear % 400) > 0 ? 0 : 1)));

		if((m_nMonth > 0) && (m_nDom > 0) && (m_nDoy == 0)){
			if(m_nDom > TimeUtil.daysInMonth[iLeap][m_nMonth]){
				throw new java.text.ParseException("day of month too high in '" + s + "'", 0);
			}
			m_nDoy = TimeUtil.dayOffset[iLeap][m_nMonth] + m_nDom;
		}
		else if((m_nDoy > 0) && (m_nMonth == 0) && (m_nDom == 0)){
			if(m_nDoy > (365 + iLeap)){
				throw new java.text.ParseException("day of year too high in '" + s + "'", 0);
			}
			int i = 2;
			while(i < 14 && m_nDoy > TimeUtil.dayOffset[iLeap][i]) i++;
			i--;
			m_nMonth = i;
			m_nDom = m_nDoy - TimeUtil.dayOffset[iLeap][i];
		}
		else{
			if(m_nMonth == 0){
				m_nMonth = 1;
			}
			m_nDom = 1;
		}

		// Okay, hit nomalize, looking at the code above, we know that the seconds
		// field can be way over value, others may be too.
		normalize();
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Construct from a datum.
	 * splits the time location datum into y,m,d,etc components.  Note that
    * seconds is a double, and micros will be 0.
	 * 
	 * @param datum with time location units
	 */
	public CalendarTime(Datum datum){
		double microseconds = TimeUtil.getMicroSecondsSinceMidnight(datum);
		Datum sansMicros = datum.subtract(microseconds, Units.microseconds);

		int jd = TimeUtil.getJulianDay(sansMicros);
		if(jd < 0)
			throw new IllegalArgumentException("julian day is negative.");
		
		int[] lDate = TimeUtil.julianToGregorian(jd);
		m_nYear = lDate[0];
		m_nMonth = lDate[1];
		m_nDom = lDate[2];
		m_nNanoSecond = Math.round( microseconds * 1000);
		normalize();
	}

	////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString(){
		return String.format("%04d-%02d-%02dT%02d:%02d%02d.%09d", m_nYear, m_nMonth,
			                  m_nDom, m_nHour, m_nMinute, m_nSecond, m_nNanoSecond);
	}

	public boolean isLeapYear(){ 	return TimeUtil.isLeapYear(m_nYear); }

	@Override
	public int compareTo(CalendarTime o){
		if(m_nYear != o.m_nYear)     return m_nYear - o.m_nYear;
		if(m_nMonth != o.m_nMonth)   return m_nMonth - o.m_nMonth;
		if(m_nDom != o.m_nDom)       return m_nDom - o.m_nDom;
		if(m_nHour != o.m_nHour)     return m_nHour - o.m_nHour;
		if(m_nMinute != o.m_nMinute) return m_nMinute - o.m_nMinute;
		if(m_nSecond != o.m_nSecond) return m_nSecond - o.m_nSecond;
		if(m_nNanoSecond < o.m_nNanoSecond) return -1;
		if(m_nNanoSecond > o.m_nNanoSecond) return 1;

		return 0;
	}

	@Override
	public boolean equals(Object o){
		if(! (o instanceof CalendarTime) ) return false;

		CalendarTime ctO = (CalendarTime)o;

		if(m_nYear != ctO.m_nYear)     return false;
		if(m_nMonth != ctO.m_nMonth)   return false;
		if(m_nDom != ctO.m_nDom)       return false;
		if(m_nHour != ctO.m_nHour)     return false;
		if(m_nMinute != ctO.m_nMinute) return false;
		if(m_nSecond != ctO.m_nSecond) return false;
		if(m_nNanoSecond != ctO.m_nNanoSecond) return false;
		
		return true;
	}

	@Override
	public int hashCode(){
		int hash = 7;
		hash = 71 * hash + this.m_nYear;
		hash = 71 * hash + this.m_nMonth;
		hash = 71 * hash + this.m_nDom;
		hash = 71 * hash + this.m_nHour;
		hash = 71 * hash + this.m_nMinute;
		hash = 71 * hash + this.m_nSecond;
		hash = 71 * hash + (int) (this.m_nNanoSecond ^ (this.m_nNanoSecond >>> 32));
		return hash;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Resolution flags to use when requesting time point as a string */
	public static enum Resolution {
		YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISEC, MICROSEC, NANOSEC
	};

	////////////////////////////////////////////////////////////////////////////////////
	/** Produce an ISO 8601 Date-Time string with up-to nanosecond resolution.
	 * The primary ISO format uses YYYY-MM-DD style dates.
	 * 
	 * @param res The resolution of the returned string.
	 * @return
	 */
	public String toISO8601(Resolution res){
		switch(res){
		case YEAR:
			return String.format("%04d", m_nYear);
		case MONTH:
			return String.format("%04d-%02d", m_nYear, m_nMonth);
		case DAY:
			return String.format("%04d-%02d-%02d", m_nYear, m_nMonth, m_nDom);
		case HOUR:
			return String.format("%04d-%02d-%02dT%02d", m_nYear, m_nMonth, m_nDom, m_nHour);
		case MINUTE:
			return String.format("%04d-%02d-%02dT%02d:%02d", m_nYear, m_nMonth, m_nDom, m_nHour, m_nMinute);
		case SECOND:
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d", m_nYear, m_nMonth, m_nDom, m_nHour,
				                  m_nMinute, m_nSecond);
		case MILLISEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03.0f", m_nYear, m_nMonth, m_nDom,
				                  m_nHour, m_nMinute, m_nSecond, m_nNanoSecond / 1000000.0);
		case MICROSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%06.0f", m_nYear, m_nMonth, m_nDom,
				                  m_nHour, m_nMinute, m_nSecond, m_nNanoSecond / 1000.0);
		case NANOSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%09d", m_nYear, m_nMonth, m_nDom,
				                  m_nHour, m_nMinute, m_nSecond, m_nNanoSecond);
		}
		return null; // added to make the compilier happy.
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Produce an alternate ISO 8601 Date-Time string with up-to nanosecond resolution.
	 * The alternate format ISO format uses ordinal day-of-year style dates, i.e.
	 * YYYY-DDD.
	 *
	 * @param res The resolution of the returned string.
	 * @return
	 */
	public String toAltISO8601(Resolution res){
		switch(res){
		case YEAR:
			return String.format("%04d", m_nYear);
		case MONTH:
			throw new IllegalArgumentException("Alternate ISO time point format doesn't "
				+ "contain a month number.");
		case DAY:
			return String.format("%04d-%03d", m_nYear, m_nDoy);
		case HOUR:
			return String.format("%04d-%03dT%02d", m_nYear, m_nDoy, m_nHour);
		case MINUTE:
			return String.format("%04d-%03dT%02d:%02d", m_nYear, m_nDoy, m_nHour, m_nMinute);
		case SECOND:
			return String.format("%04d-%03dT%02d:%02d:%02d", m_nYear, m_nDoy, m_nHour,
				                  m_nMinute, m_nSecond);
		case MILLISEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%03.0f", m_nYear, m_nDoy, m_nHour,
				                  m_nMinute, m_nSecond, m_nNanoSecond / 1000000.0);
		case MICROSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%06.0f", m_nYear, m_nDoy, m_nHour,
				                  m_nMinute, m_nSecond, m_nNanoSecond / 1000.0);
		case NANOSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%09d", m_nYear, m_nDoy, m_nHour,
				                  m_nMinute, m_nSecond, m_nNanoSecond);
		}
		return null; // added to make the compilier happy.
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Normalize date and time components for the Gregorian calendar ignoring leap seconds
	 *
	 * From Larry Granroth's original Das1 libs.
	 */
	private void normalize(){

		// month is required input -- first adjust month
		if( m_nMonth > 12 || m_nMonth < 1){
			// temporarily make month zero-based
			m_nMonth--;
			m_nYear += m_nMonth / 12;
			m_nMonth %= 12;
			if(m_nMonth < 0){
				m_nMonth += 12;
				m_nYear--;
			}
			m_nMonth++;
		}

		// index for leap year
		int iLeap = TimeUtil.isLeapYear(m_nYear)?1:0;

		// day of year is output only -- calculate it
		m_nDoy = TimeUtil.dayOffset[iLeap][m_nMonth] + m_nDom;

		// now adjust other items . . .

		// New addition, handle nanoseconds
		if(m_nNanoSecond >= 1000000000 || m_nNanoSecond < 0){
			m_nSecond += m_nNanoSecond / 1000000000;
			m_nNanoSecond = m_nNanoSecond % 1000000000;
			if(m_nNanoSecond < 0){
				m_nNanoSecond += 1000000000;
				m_nSecond--;
			}
		}

		// again, we're ignoring leap seconds
		if( m_nSecond >= 60 || m_nSecond < 0){
			m_nMinute += m_nSecond / 60;
			m_nSecond = m_nSecond % 60;
			if(m_nSecond < 0){
				m_nSecond += 60;
				m_nMinute--;
			}
		}

		if(m_nMinute >= 60 || m_nMinute < 0){
			m_nHour += m_nMinute / 60;
			m_nMinute %= 60;
			if(m_nMinute < 0){
				m_nMinute += 60;
				m_nHour--;
			}
		}

		if(m_nHour >= 24 ||m_nHour < 0){
			m_nDoy += m_nHour / 24;
			m_nHour %= 24;
			if(m_nHour < 0){
				m_nHour += 24;
				m_nDoy--;
			}
		}

		/* final adjustments for year and day of year */
		int ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
		if(m_nDoy > ndays || m_nDoy < 1){
			while(m_nDoy > ndays){
				m_nYear++;
				m_nDoy -= ndays;
				ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
			}
			while(m_nDoy < 1){
				m_nYear--;
				ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
				m_nDoy += ndays;
			}
		}

		/* and finally convert day of year back to month and day */
		iLeap = TimeUtil.isLeapYear(m_nYear)?1:0;
		while(m_nDoy <= TimeUtil.dayOffset[iLeap][m_nMonth]){
			m_nMonth--;
		}
		while(m_nDoy > TimeUtil.dayOffset[iLeap][m_nMonth + 1]){
			m_nMonth++;
		}
		m_nDom = m_nDoy - TimeUtil.dayOffset[iLeap][m_nMonth];
	}

	////////////////////////////////////////////////////////////////////////////////////

	/** Set the year field.  Use set() if you have multiple fields to set. */
	public void setYear(int nYear){
		m_nYear = nYear;
		normalize();
	}
	/** Set the month field.  Use set() if you have multiple fields to set. */
	public void setMonth(int nMonth){
		m_nMonth = nMonth;
		normalize();
	}
	/** Set the day of month field.  Use set() if you have multiple fields to set. */
	public void setDay(int nDay){
		m_nDom = nDay;
		normalize();
	}

	/** Set the day of year, and recompute the month and day of month.
	 * @param nDoy the new day of year.
	 */
	public void setDayOfYear(int nDoy){
		m_nDoy = nDoy;

		/* final adjustments for year and day of year */
		int ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
		if(m_nDoy > ndays || m_nDoy < 1){
			while(m_nDoy > ndays){
				m_nYear++;
				m_nDoy -= ndays;
				ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
			}
			while(m_nDoy < 1){
				m_nYear--;
				ndays = TimeUtil.isLeapYear(m_nYear) ? 366 : 365;
				m_nDoy += ndays;
			}
		}

		/* and finally convert day of year back to month and day */
		int iLeap = TimeUtil.isLeapYear(m_nYear)?1:0;
		while(m_nDoy <= TimeUtil.dayOffset[iLeap][m_nMonth]){
			m_nMonth--;
		}
		while(m_nDoy > TimeUtil.dayOffset[iLeap][m_nMonth + 1]){
			m_nMonth++;
		}
		m_nDom = m_nDoy - TimeUtil.dayOffset[iLeap][m_nMonth];
	}

	/** Set the hour field.  Use set() if you have multiple fields to set. */
	public void setHour(int nHour){
		m_nHour = nHour;
		normalize();
	}
	/** Set the minute field.  Use set() if you have multiple fields to set. */
	public void setMinute(int nMinute){
		m_nMinute = nMinute;
		normalize();
	}
	/** Set the second field.  Use set() if you have multiple fields to set. */
	public void setSecond(int nSecond){
		m_nSecond = nSecond;
		normalize();
	}
	/** Set the nanosecond field.  Use set() if you have multiple fields to set. */
	public void setNanoSecond(long nNano){
		m_nNanoSecond = nNano;
		normalize();
	}

	/** Set (upto) all fields of a calendar time
	 *
	 * @param lFields An array of 1 to 7 items whose values will be assigned to the
	 *        year, month, day, hour, minute, second and nanosecond respectively.  Also
	 *        integers can be specified one at a time in var-args fashion
	 */
	public void set(int... lFields){

		if(lFields.length < 1) return;

		// Cool case fall through (good idea Ed!)
		switch(lFields.length){
		default: m_nNanoSecond = lFields[6];
		case 6:  m_nSecond = lFields[5];
		case 5:  m_nMinute = lFields[4];
		case 4:  m_nHour = lFields[3];
		case 3:  m_nDom = lFields[2];
		case 2:  m_nMonth = lFields[1];
		case 1:  m_nYear = lFields[0];
		}
		
		normalize();
	}

	/////////////////////////////////////////////////////////////////////////////////////
	
	public int year(){return m_nYear;}
	public int month(){return m_nMonth;}
	public int day(){return m_nDom;}
	public int dayOfYear(){	return m_nDoy;	}
	public int hour(){return m_nHour;}
	public int minute(){return m_nMinute;}
	public int second(){return m_nSecond;}
	public int nanosecond(){return (int)m_nNanoSecond;}

	public int[] get(){
		return new int[]{m_nYear, m_nMonth, m_nDom, m_nHour, m_nMinute, m_nSecond,
		                 (int)m_nNanoSecond};
	}


	////////////////////////////////////////////////////////////////////////////////////

	/** Used to step add to a calendar time by 1 or more integer units. */
	public enum Step {
		YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, NANOSEC, HALF_YEAR, QUARTER,
		MILLISEC, MICROSEC;

		public static Step HigerStep(Step step){
			switch(step){
			case DAY: return MONTH;
			case HOUR: return DAY;
			case MINUTE: return HOUR;
			case SECOND: return MINUTE;
			case MILLISEC: return SECOND;
			case MICROSEC: return MILLISEC;
			case NANOSEC: return MICROSEC;
			default: return YEAR;
			}
		}

		public static Step LowerStep(Step step){
			switch(step){
			case YEAR: return MONTH;
			case MONTH: return DAY;
			case DAY: return HOUR;
			case HOUR: return MINUTE;
			case MINUTE: return SECOND;
			case SECOND: return MILLISEC;
			case MILLISEC: return MICROSEC;
			default: return NANOSEC;
			}
		}
	}

	/** A convenience method for handling multiple steps at once.
	 *
	 * @param steps An array of upto 7 items, each one will step succeedingly smaller
	 *        time fields if present.  So the array items are taken to be:
	 *        [year, month, day, hour, minute, second, nanosecond].
	 *
	 * @return A new calendar time stepped as specified.
	 */
	public CalendarTime step(int lSteps[]){
		CalendarTime ct = new CalendarTime(this);

		Step fields[] = {Step.YEAR, Step.MONTH, Step.DAY, Step.HOUR, Step.MINUTE,
		                 Step.SECOND, Step.NANOSEC};
		for(int i = 0; i < 7; i++){
			if((lSteps.length > i)&&(lSteps[i] != 0))
				ct = ct.step(fields[i], lSteps[i]);
		}

		return ct;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Introduced as a way to increase the efficiency of the time axis tick calculation.
	 * Step to the next higher ordinal.  If the calendar time is already at the ordinal,
	 * then step by one unit.
	 * 
    * @param field The time field to change.  Integers for this value are defined in
	 *        TimeUtil
    * @param steps number of positive or negative steps to take
    * @return
    */
    public CalendarTime step(Step field, int steps) {

		CalendarTime ct = new CalendarTime(this);

		if(steps == 0) return ct;

		// First change the relavent field
      switch(field){
		case NANOSEC:   ct.m_nNanoSecond += steps; break;
		case MICROSEC:  ct.m_nNanoSecond += 1000*steps; break;
		case MILLISEC:  ct.m_nNanoSecond += 1000000*steps; break;
		case SECOND:    ct.m_nSecond += steps; break;
		case MINUTE:    ct.m_nMinute += steps; break;
		case HOUR:      ct.m_nHour += steps; break;
		case DAY:       ct.m_nDom += steps; break;
		case MONTH:     ct.m_nMonth += steps; break;
		case QUARTER:   ct.m_nMonth += steps*3; break;
		case HALF_YEAR: ct.m_nMonth += steps*6; break;
		case YEAR:      ct.m_nYear += steps*1; break;
		default:
			throw new IllegalArgumentException("Unknown time field designator: "+field);
		}
		

		// Handle zeroing out lower level fields (case fall throught can be handy)
		switch(field){
		case YEAR:
			ct.m_nMonth = 1;
		case HALF_YEAR:
			//Map months to a 0-1 half year scale
			double dHalfYears = (ct.m_nMonth - 1)/6.0;
			ct.m_nMonth = (((int)dHalfYears) * 6) + 1;  //Truncates towards zero
		case QUARTER:
			//Map months to a 0-3 quarterly scale
			double dQuarters = (ct.m_nMonth - 1)/3.0;
			ct.m_nMonth = (((int)dQuarters) * 3) + 1;   //Truncates towards zero
		case MONTH:
			ct.m_nDom = 1;
		case DAY:
			ct.m_nHour = 0;
		case HOUR:
			ct.m_nMinute = 0;
		case MINUTE:
			ct.m_nSecond = 0;
		case MILLISEC:
			ct.m_nNanoSecond = (ct.m_nNanoSecond / 1000000) * 1000000;
		case MICROSEC:
			ct.m_nNanoSecond = (ct.m_nNanoSecond / 1000) * 1000;
		}

		 ct.normalize();
		 return ct;
    }

	/** Special handler for changing the nanoseconds, as this field is a long */
	public CalendarTime stepNano(long steps) {
		CalendarTime ct = new CalendarTime(this);
		ct.m_nNanoSecond += steps;
		ct.normalize();
		return ct;
	}


	 ///////////////////////////////////////////////////////////////////////////////////
	 /** Get a time datum in us2000 units.
	  *
	  * @return A datum whose value is the number of milliseconds since midnight
	  * 2000-01-01, ignoring leap seconds.
	  */
	 public Datum toDatum(){
		 int jd = 367 * m_nYear - 7 * (m_nYear + (m_nMonth + 9) / 12) / 4
			 - 3 * ((m_nYear + (m_nMonth - 9) / 7) / 100 + 1) / 4
			 + 275 * m_nMonth / 9 + m_nDom + 1721029;
		 
		 double us2000 = (jd - 2451545) * 86400e6; // TODO: leap seconds

		 return Datum.create(m_nHour*3600.0e6 + m_nMinute*60e6 + m_nSecond*1e6
			                  + m_nNanoSecond/1000 + us2000, Units.us2000);
	 }
	 

}
