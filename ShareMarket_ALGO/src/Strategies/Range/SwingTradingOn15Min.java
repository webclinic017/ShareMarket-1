package Strategies.Range;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.poi.hssf.record.DBCellRecord;

import Indicators.Connection;
import Indicators.Test;

public class SwingTradingOn15Min extends Connection{

	   public static void main(String[] args)  {
		      Test t = new Test();
		      SwingTradingOn15Min range = new SwingTradingOn15Min();
		      java.sql.Connection dbConnection = null;
		      boolean updateSymbolsTableData=true; boolean updateAllData=true;
		      try{
		    	  
		    	  Connection con = new Connection();
		    	  	dbConnection = con.getDbConnection();
		    	  	ResultSet rs=null;
		    	  	rs = con.executeSelectSqlQuery(dbConnection, "SELECT s.name FROM symbols s, margintables m where m.name=s.name  and volume > 100000000 and "
		    	  			+ "s.name<>'Mindtree' order by volume desc ");
		    	  	String name="";
		    	  	boolean updateForTodayAndNextDay=true; boolean updateForallDays=true;
		    	  	boolean updateResultTable=false;boolean isIntraDayData=false;
		    	  	boolean insertAllDataToResult = false;
		    	  	String iter="1d";
		    	  	String path="C:/Puneeth/SHARE_MARKET/Hist_Data/Intraday/";
		    	  	while (rs.next()){
		    	  		name= rs.getString("s.name");
		    	  		System.out.println(name);
		    	  		if(!iter.equals("1d"))
		    	  			name =name+"_"+iter+"";
		    	  		range.getdate(dbConnection, name);
		    	  	}
		      }
			  	
		      catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		      catch(Exception e){
					e.printStackTrace();
				}
		      finally{
		  	  	if(dbConnection!=null)
					try {
						dbConnection.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
		      }
	   }
	   
	   public void getdate(java.sql.Connection con, String name) throws SQLException{
		   String sql="Select * from "+name+" where tradedate>'2017-02-01'";
		   ResultSet rs=null;
		   rs = executeSelectSqlQuery(con, sql);

		  	List open = new ArrayList<Float>();
		  	List high = new ArrayList<Float>();
		  	List low = new ArrayList<Float>();
		  	List close = new ArrayList<Float>();
		  	List date = new ArrayList<String>();
		  	List volume = new ArrayList<Integer>();
		   while (rs.next()){
			   open.add(rs.getFloat("open"));
		  		high.add(rs.getFloat("high"));
		  		low.add(rs.getFloat("low"));
		  		close.add(rs.getFloat("close"));
		  		date.add(rs.getString("tradedate"));
		   }
		   for(int i=1; i< date.size(); i++){
//			   if(((float)open.get(i)-(float)close.get(i-1)) *100/(float)close.get(i-1) > .1)
//				   UpdatePinResultsRange(con, name, date.get(i).toString(), "Up");
//			   else if(((float)close.get(i-1)-(float)open.get(i)) *100/(float)close.get(i-1) > .1)
//				   UpdatePinResultsRange(con, name, date.get(i).toString(), "Down");
			   UpdatePinResultsRange(con, name, date.get(i).toString(),(float)close.get(i), "Down");
		   }
		   rs.close();
	   }
	   
	   public void UpdatePinResultsRange(java.sql.Connection con, String name,String dailyDate, float daysClose, String dir) throws SQLException {
		   ResultSet rs=null;
		   List open = new ArrayList<Float>();
		  	List high = new ArrayList<Float>();
		  	List low = new ArrayList<Float>();
		  	List close = new ArrayList<Float>();
		  	List date = new ArrayList<String>();
		  	String sql="";String interval="15";
		  	try {
		  		if(!interval.equals("")){
		  			sql="select * from "+name+"_"+interval+" where tradedate >=concat(Date('"+dailyDate+" '),' 9:10:00') and tradedate <= concat(Date('"+dailyDate+"'),' 15:00:00')";
		  		}else{
		  			sql="select * from "+name+" where tradedate >=concat(Date('"+dailyDate+" '),' 9:10:00') and tradedate <= concat(Date('"+dailyDate+"'),' 15:00:00')";
		  		}
		  		
				rs = executeSelectSqlQuery(con, sql);
			  	while (rs.next()){
			  		open.add(rs.getFloat("open"));
			  		high.add(rs.getFloat("high"));
			  		low.add(rs.getFloat("low"));
			  		close.add(rs.getFloat("close"));
			  		date.add(rs.getString("tradedate"));
			  	}
			  	
			  	String tableName = "williamsresults";
			  	float filter=0f, filterPerc = 2f, rangeHigh=0f, rangeLow=0f;
			  	Float profitPerc=0.0f, profitRupees=0.0f;
			  	float range=0f;int x=0; float p=1f, l=-2f, bullTriggerPrice=0f, bearTriggerPrice=0f, dailyRange=0f, stopLossPrice=0f, stopLoss=3f;
			  	String tradeDate="";float dayshigh=0, dayslow=0; String exitPrice=""; 
			  	String daysOpen="";int maxVolume=0;
			  	
				 sql = "select coalesce(max(volume),0) as maxVolume from "+name+"_"+interval+" where tradedate >=concat(Date('"+dailyDate+" '),' 9:10:00') and tradedate <= concat(Date('"+dailyDate+"'),' 09:40:00')";
				 maxVolume = Integer.parseInt(executeCountQuery(con, sql));
				 
				 float pivotRange=0f;
				 daysOpen = executeCountQuery(con, "select open from "+name+" where tradedate=Date('"+dailyDate+"')");
			  		for (int i=0; i< date.size()-1; i++){
			  			dailyRange = ((float)close.get(i)-(float)open.get(i))*100/(float)open.get(i);
			  			if(dailyRange>2){
			  				bullTriggerPrice = (float)low.get(i);
//			  				bullTriggerPrice = (float) (bullTriggerPrice - (bullTriggerPrice*0.5/100));
			  				continue;
			  			}
			  			if((float)low.get(i) <= bullTriggerPrice && bullTriggerPrice!=0 ){
			  				sql = "select max(high) from "+name+"_"+interval+" where tradedate >'"+date.get(i)+"' and tradedate <= concat(Date('"+date.get(i)+"'),' 15:30:00')";
	          				dayshigh = Float.parseFloat(executeCountQuery(con, sql));
//	          				bullTriggerPrice = (float)open.get(i+1);
			  				profitPerc = (dayshigh-bullTriggerPrice)*100/bullTriggerPrice;
							sql = "insert into williamsresults(name, reversal, triggerPrice, profitPerc, profitRupees, date) "
				  					+ " values ('"+name+"', 'Bull', "+profitPerc+", "+profitPerc+", "+profitPerc+", '"+date.get(i)+"')";
							executeSqlQuery(con, sql);
							break;
			  			}
			  			
			  			dailyRange = ((float)open.get(i)-(float)close.get(i))*100/(float)open.get(i);
			  			if(dailyRange>2){
			  				bearTriggerPrice = (float)high.get(i);
//			  				bearTriggerPrice = (float) (bearTriggerPrice + (bearTriggerPrice*0.5/100));
			  				continue;
			  			}
			  			if((float)high.get(i) >= bearTriggerPrice && bearTriggerPrice!=0){
			  				sql = "select min(low) from "+name+"_"+interval+" where tradedate >'"+date.get(i)+"' and tradedate <= concat(Date('"+date.get(i)+"'),' 15:30:00')";
	          				dayslow = Float.parseFloat(executeCountQuery(con, sql));
//	          				bearTriggerPrice = (float)open.get(i+1);
			  				profitPerc = (bearTriggerPrice-dayslow)*100/bearTriggerPrice;
							sql = "insert into williamsresults(name, reversal, triggerPrice, profitPerc, profitRupees, date) "
				  					+ " values ('"+name+"', 'Bear', "+profitPerc+", "+profitPerc+", "+profitPerc+", '"+date.get(i)+"')";
							executeSqlQuery(con, sql);
							break;
			  			}
			  		}
			  	  	
			  	if(rs!=null) rs.close();
		  	}
		  	catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  	catch(Exception e){
		    	  e.printStackTrace();
		    }
		  	finally{
		  		open.clear();high.clear();low.clear();close.clear();date.clear();
		  		rs.close();
		  	}
	   }
}
