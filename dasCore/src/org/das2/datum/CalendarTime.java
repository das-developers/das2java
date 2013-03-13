package org.das2.datum;

import java.text.ParseException;
import java.util.StringTokenizer;

/** Represents a point in time, over thousands of years to nano-second resolution.
 * The Gegorian calendar is extended in both directions, which makes little sense
 * for dates prior to it's adoption.
 * 
 * @author ljg, eew, jbf, cwp
 */
public final class CalendarTime{

	/** The time point's year number.
	 *  Note: that year 1 BC is represented year 0 in this field.
	 */
	public int year;

	/** The time point's month of year, normalized range is 1 to 12 */
	public int month;

	/** The time point's day of month, normalized range is 1 up to 31
	 * depending on the month rValue.
	 */
	public int day;

	// Cash the day of year calculation after a normalize.
	private int m_nDoy;
	
	/** The time point's hour of day, normalized range is 0 to 23 */
	public int hour;

	/** The time point's minute of hour, normalized range is 0 to 59 */
	public int minute;

	/** The time point's second of minute, normalized range is 0 to 59.
	 * Note that leap seconds are <b>not</b> handled by this class, though it
	 * wouldn't be hard to do so.
	 */
	public int second;

	/** The time point's nanosecond of second, normalized range is 0 to 999,999,999 */
	public long nanosecond;


	////////////////////////////////////////////////////////////////////////////////////
	/** Empty constructor */

	public CalendarTime(){
		year = 1;
		month = 1;
		day = 1;
		m_nDoy = 1;
		hour = 0;
		minute = 0;
		second = 0;
		nanosecond = 0;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Tuple constructor
	 * @param lFields an array of integer fields.  Values are assumed to be in largest
	 *        time span to smallest time span.  The array may be null.  Up to 7 items will
	 *        be used from the array in the order: year, month, day, hour, min, sec,
	 *        nanosecond.
	 */
	public CalendarTime(int[] lFields){
		year = 1;
		month = 1;
		day = 1;
		m_nDoy = 1;
		hour = 0;
		minute = 0;
		second = 0;
		nanosecond = 0;

		if(lFields == null) return;
		if(lFields.length > 0) year = lFields[0];
		if(lFields.length > 1) month = lFields[1];
		if(lFields.length > 2) day = lFields[2];
		if(lFields.length > 3) hour = lFields[3];
		if(lFields.length > 4) minute = lFields[4];
		if(lFields.length > 5) second = lFields[5];
		if(lFields.length > 6) nanosecond = lFields[6];

		normalize();
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Copy constructor */
	public CalendarTime(CalendarTime other){
		year = other.year;
		month = other.month;
		day = other.day;
		m_nDoy = other.m_nDoy;
		hour = other.hour;
		minute = other.minute;
		second = other.second;
		nanosecond = other.nanosecond;
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
		year = 0;
		month = 0;
		day = 0;
		m_nDoy = 0;
		hour = 0;
		minute = 0;
		second = 0;
		nanosecond = 0;

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
						month = j + 1;
						lWant[MONTH] = false;
						if(hold > 0){
							if(day > 0)
								throw new ParseException("Ambiguous dates in token '" + lToks[i] +
									                      "' in '" + s + "'", 0);
							day = hold;
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
					second = (int) nTmp / 1000000000;
					nanosecond = nTmp % 1000000000;
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
					year = number / 10000;
					lWant[YEAR] = false;
					month = number / 100 % 100;
					lWant[MONTH] = false;
					day = number % 100;
					m_nDoy = 0;
					lWant[DAY] = false;
				}
				else if(number >= 1000000 && lWant[YEAR]){ //%Y%j
					year = number / 1000;
					lWant[YEAR] = false;
					m_nDoy = number % 1000;
					month = 0;
					lWant[MONTH] = false;
					lWant[DAY] = false;

				}
				else if(number > 31){

					if(lWant[YEAR]){
						year = number;
						if(year < 1000){
							year += 1900;
						}
						lWant[YEAR] = false;
					}
					else if(lWant[MONTH]){
						lWant[MONTH] = false;
						month = 0;
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
							month = hold;
							lWant[MONTH] = false;
						}
						if(nTokLen == 3){
							if(month > 0){
								throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
							}
							m_nDoy = number;
							day = 0;
							lWant[MONTH] = false;
						}
						else{
							day = number;
						}
						lWant[DAY] = false;
					}
					else{
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}

				}
				else if(!lWant[MONTH]){

					if(month > 0){
						day = number;
						m_nDoy = 0;
					}
					else{
						m_nDoy = number;
						day = 0;
					}
					lWant[DAY] = false;

				}
				else if(!lWant[DAY]){

					if(m_nDoy > 0){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					month = number;
					lWant[MONTH] = false;

				}
				else if(!lWant[YEAR]){

					if(nTokLen == 3){
						if(month > 0){
							throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
						}
						m_nDoy = number;
						day = 0;
						lWant[DAY] = false;
					}
					else{
						if(m_nDoy > 0){
							throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
						}
						month = number;
						if(hold > 0){
							day = hold;
							lWant[DAY] = false;
						}
					}
					lWant[MONTH] = false;

				}
				else if(hold > 0){

					month = hold;
					hold = 0;
					lWant[MONTH] = false;
					day = number;
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
					hour = hold;
					hold = number % 100;
					if(hold > 59){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					minute = hold;
					lWant[MINUTE] = false;
				}
				else{
					if(number > 23){
						throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
					}
					hour = number;
				}
				lWant[HOUR] = false;

			}
			else if(lWant[MINUTE]){
				// TODO: handle times like 0:90 --> 1:30,  for ease of modifying times
				if(number > 59){
					throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
				}
				minute = number;
				lWant[MINUTE] = false;

			}
			else if(lWant[SECOND]){

				if(number > 61){
					throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
				}
				second = number;
				lWant[SECOND] = false;

			}
			else{
				throw new ParseException("Error at token '" + lToks[i] + "' in '" + s + "'", 0);
			}

		}
		// End of token parsing loop

		if(month > 12){
			throw new ParseException("Month is greater than 12 in '" + s + "'", 0);
		}
		if(month > 0 && day <= 0){
			day = 1;
		}

		int iLeap = ((year % 4) != 0 ? 0 : ((year % 100) > 0 ? 1 : ((year % 400) > 0 ? 0 : 1)));

		if((month > 0) && (day > 0) && (m_nDoy == 0)){
			if(day > TimeUtil.daysInMonth[iLeap][month]){
				throw new java.text.ParseException("day of month too high in '" + s + "'", 0);
			}
			m_nDoy = TimeUtil.dayOffset[iLeap][month] + day;
		}
		else if((m_nDoy > 0) && (month == 0) && (day == 0)){
			if(m_nDoy > (365 + iLeap)){
				throw new java.text.ParseException("day of year too high in '" + s + "'", 0);
			}
			int i = 2;
			while(i < 14 && m_nDoy > TimeUtil.dayOffset[iLeap][i]) i++;
			i--;
			month = i;
			day = m_nDoy - TimeUtil.dayOffset[iLeap][i];
		}
		else{
			if(month == 0){
				month = 1;
			}
			day = 1;
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
		year = lDate[0];
		month = lDate[1];
		day = lDate[2];
		nanosecond = Math.round( microseconds * 1000);
		normalize();
	}

	////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString(){
		return year + "/" + month + "/" + day + " " + hour + ":" + minute + ":" + second +
			    "." + nanosecond;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Get the computed Day of Year for the current date
	 *
	 * @return The Day of year which is a value from 1 - 366 when the calendar time is
	 *         normalized.
	 */
	public int getDoy(){
		return m_nDoy;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Set the day of year, and recompute the month and day of month.
	 * @param nDoy the new day of year.
	 * @throws  IllegalArgumentException if the value is outside the range 1 to 365 (or
	 *          366 on a leap year)
	 */
	public void setDoy(int nDoy){
		if( (nDoy < 1) || ((!TimeUtil.isLeapYear(year)) && (nDoy > 365)) ||
			 (nDoy > 366))
			throw new IllegalArgumentException("Day of year value "+nDoy+" is out of range.");

		m_nDoy = nDoy;

		int iLeap = TimeUtil.isLeapYear(year)?1:0;
		while(m_nDoy <= TimeUtil.dayOffset[iLeap][month]){
			month--;
		}
		while(m_nDoy > TimeUtil.dayOffset[iLeap][month + 1]){
			month++;
		}
		day = m_nDoy - TimeUtil.dayOffset[iLeap][month];

	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Resolution flags to use when requesting time point as a string */
	static public enum RESOLUTION {
		YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, MILLISEC, MICROSEC, NANOSEC
	};

	////////////////////////////////////////////////////////////////////////////////////
	/** Produce an ISO 8601 Date-Time string with up-to nanosecond resolution.
	 * The primary ISO format uses YYYY-MM-DD style dates.
	 * 
	 * @param res The resolution of the returned string.
	 * @return
	 */
	public String toISO8601(RESOLUTION res){
		switch(res){
		case YEAR:
			return String.format("%04d", year);
		case MONTH:
			return String.format("%04d-%02d", year, month);
		case DAY:
			return String.format("%04d-%02d-%02d", year, month, day);
		case HOUR:
			return String.format("%04d-%02d-%02dT%02d", year, month, day, hour);
		case MINUTE:
			return String.format("%04d-%02d-%02dT%02d:%02d", year, month, day, hour, minute);
		case SECOND:
			return String.format("%04d-%02d-%02dT%02d:%02d:02d%", year, month, day, hour,
				                  minute, second);
		case MILLISEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%03.0f", year, month, day,
				                  hour, minute, second, nanosecond / 1000000.0);
		case MICROSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%06.0f", year, month, day,
				                  hour, minute, second, nanosecond / 1000.0);
		case NANOSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%02d-%02dT%02d:%02d:%02d.%09d", year, month, day,
				                  hour, minute, second, nanosecond);
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
	public String toAltISO8601(RESOLUTION res){
		switch(res){
		case YEAR:
			return String.format("%04d", year);
		case MONTH:
			throw new IllegalArgumentException("Alternate ISO time point format doesn't "
				+ "contain a month number.");
		case DAY:
			return String.format("%04d-%03d", year, m_nDoy);
		case HOUR:
			return String.format("%04d-%03dT%02d", year, m_nDoy, hour);
		case MINUTE:
			return String.format("%04d-%03dT%02d:%02d", year, m_nDoy, hour, minute);
		case SECOND:
			return String.format("%04d-%03dT%02d:%02d:02d%", year, m_nDoy, hour,
				                  minute, second);
		case MILLISEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%03.0f", year, m_nDoy, hour,
				                  minute, second, nanosecond / 1000000.0);
		case MICROSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%06.0f", year, m_nDoy, hour,
				                  minute, second, nanosecond / 1000.0);
		case NANOSEC:
			// Let string.format handle rounding for me.
			return String.format("%04d-%03dT%02d:%02d:%02d.%09d", year, m_nDoy, hour, 
				                  minute, second, nanosecond);
		}
		return null; // added to make the compilier happy.
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Add a time span to this time point */
	public CalendarTime add(TimeDifference offset){
		
		CalendarTime result = new CalendarTime(this);
		result.day += offset.days;
		result.hour += offset.hours;
		result.minute += offset.minutes;
		result.second += offset.seconds;
		result.nanosecond += offset.nanoseconds;
		result.normalize();
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////
	/** Normalize date and time components for the Gregorian calendar ignoring leap seconds
	 *
	 * From Larry Granroth's original Das1 libs.
	 */
	public void normalize(){

		// month is required input -- first adjust month
		if( month > 12 || month < 1){
			// temporarily make month zero-based
			month--;
			year += month / 12;
			month %= 12;
			if(month < 0){
				month += 12;
				year--;
			}
			month++;
		}

		// index for leap year
		int iLeap = TimeUtil.isLeapYear(year)?1:0;

		// day of year is output only -- calculate it
		m_nDoy = TimeUtil.dayOffset[iLeap][month] + day;

		// now adjust other items . . .

		// New addition, handle nanoseconds
		if(nanosecond >= 1000000000 || nanosecond < 0){
			second += nanosecond / 1000000000;
			nanosecond = nanosecond % 1000000000;
			if(nanosecond < 0){
				nanosecond += 1000000000;
				second--;
			}
		}

		// again, we're ignoring leap seconds
		if( second >= 60 || second < 0){
			minute += second / 60;
			second = second % 60;
			if(second < 0){
				second += 60;
				minute--;
			}
		}

		if(minute >= 60 || minute < 0){
			hour += minute / 60;
			minute %= 60;
			if(minute < 0){
				minute += 60;
				hour--;
			}
		}

		if(hour >= 24 ||hour < 0){
			m_nDoy += hour / 24;
			hour %= 24;
			if(hour < 0){
				hour += 24;
				m_nDoy--;
			}
		}

		/* final adjustments for year and day of year */
		int ndays = TimeUtil.isLeapYear(year) ? 366 : 365;
		if(m_nDoy > ndays || m_nDoy < 1){
			while(m_nDoy > ndays){
				year++;
				m_nDoy -= ndays;
				ndays = TimeUtil.isLeapYear(year) ? 366 : 365;
			}
			while(m_nDoy < 1){
				year--;
				ndays = TimeUtil.isLeapYear(year) ? 366 : 365;
				m_nDoy += ndays;
			}
		}

		/* and finally convert day of year back to month and day */
		iLeap = TimeUtil.isLeapYear(year)?1:0;
		while(m_nDoy <= TimeUtil.dayOffset[iLeap][month]){
			month--;
		}
		while(m_nDoy > TimeUtil.dayOffset[iLeap][month + 1]){
			month++;
		}
		day = m_nDoy - TimeUtil.dayOffset[iLeap][month];
	}

	/** Used to step a calendar time by 1 or more years */
	public static final int YEAR = 1;

	/** Used to step a calendar time by 1 or more months */
	public static final int MONTH = 2;

	/** Used to step a calendar time by 1 or more days */
	public static final int DAY = 3;

	/** Used to step a calendar time by 1 or more hours */
	public static final int HOUR = 4;

	/** Used to step a calendar time by 1 or more minutes */
	public static final int MINUTE = 5;

	/** Used to step a calendar time by 1 or more seconds */
	public static final int SECOND = 6;

	/** Used to step a calendar time by 1 or more nanoseconds */
	public static final int NANOSEC = 7;

	/** Used to step a calendar time by 1 or more half-years */
	public static final int HALF_YEAR = 101;

	/** Used to step a calendar time by 1 or more quarters */
	public static final int QUARTER = 102;

	/** Used to step a calendar time by 1 or more milliseconds */
	public static final int MILLISEC = 103;

	/** Used to step a calendar time by 1 or more microseconds */
	public static final int MICROSEC = 104;
	

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
    public CalendarTime step(int field, int steps) {

		CalendarTime ct = new CalendarTime(this);

		if(steps == 0) return ct;

		// First change the relavent field
      switch(field){
		case NANOSEC:   ct.nanosecond += steps; break;
		case MICROSEC:  ct.nanosecond += 1000*steps; break;
		case MILLISEC:  ct.nanosecond += 1000000*steps; break;
		case SECOND:    ct.second += steps; break;
		case MINUTE:    ct.minute += steps; break;
		case HOUR:      ct.hour += steps; break;
		case DAY:       ct.day += steps; break;
		case MONTH:     ct.month += steps; break;
		case QUARTER:   ct.month += steps*3; break;
		case HALF_YEAR: ct.month += steps*6; break;
		case YEAR:      ct.year += steps*1; break;
		default:
			throw new IllegalArgumentException("Unknown time field designator: "+field);
		}
		

		// Handle zeroing out lower level fields (case fall throught can be handy)
		switch(field){
		case YEAR:
			ct.month = 1;
		case HALF_YEAR:
			//Map months to a 0-1 half year scale
			double dHalfYears = (ct.month - 1)/6.0;
			ct.month = (((int)dHalfYears) * 6) + 1;  //Truncates towards zero
		case QUARTER:
			//Map months to a 0-3 quarterly scale
			double dQuarters = (ct.month - 1)/3.0;
			ct.month = (((int)dQuarters) * 3) + 1;   //Truncates towards zero
		case MONTH:
			ct.day = 1;
		case DAY:
			ct.hour = 0;
		case HOUR:
			ct.minute = 0;
		case MINUTE:
			ct.second = 0;
		case MILLISEC:
			ct.nanosecond = (ct.nanosecond / 1000000) * 1000000;
		case MICROSEC:
			ct.nanosecond = (ct.nanosecond / 1000) * 1000;
		}

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
		 int jd = 367 * year - 7 * (year + (month + 9) / 12) / 4
			 - 3 * ((year + (month - 9) / 7) / 100 + 1) / 4
			 + 275 * month / 9 + day + 1721029;
		 
		 double us2000 = (jd - 2451545) * 86400e6; // TODO: leap seconds

		 return Datum.create(hour*3600.0e6 + minute*60e6 + second*1e6
			                  + nanosecond/1000 + us2000, Units.us2000);
	 }
	 

}
