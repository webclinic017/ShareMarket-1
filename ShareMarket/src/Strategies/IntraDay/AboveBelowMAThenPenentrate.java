package Strategies.IntraDay;

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

public class AboveBelowMAThenPenentrate extends Connection{

	java.sql.Connection dbConnection = null;
	public AboveBelowMAThenPenentrate() {
		Connection con = new Connection();
	  	dbConnection = con.getDbConnection();
	}
	   public static void main(String[] args)  {
		      Test t = new Test();
		      AboveBelowMAThenPenentrate range = new AboveBelowMAThenPenentrate();
		      
		      boolean updateSymbolsTableData=true; boolean updateAllData=true;
		      try{
		    	  
		    	    Connection con = new Connection();
		    	  	ResultSet rs=null;
		    	  	String sql = "SELECT s.name FROM symbols s, margintables m where m.name=s.name  and volume > 100000 "
		    	  			+ "and s.name<>'Mindtree' and s.name<>'IOC' and s.name<>'GRANULES' order by volume desc ";
//		    	  	sql = "SELECT s.name FROM symbols s where volume > 10000000 order by volume desc ";
		    	  	rs = con.executeSelectSqlQuery(range.dbConnection, sql);
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
		    	  		range.getdate(range.dbConnection, name);
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
		      }
	   }
	   
	   public void getdate(java.sql.Connection con, String name) throws SQLException, InterruptedException{
		   String sql="Select * from "+name+"_60";
		   ResultSet rs=null;
		   rs = executeSelectSqlQuery(con, sql);

		  	List open = new ArrayList<Float>();
		  	List high = new ArrayList<Float>();
		  	List low = new ArrayList<Float>();
		  	List close = new ArrayList<Float>();
		  	List ema1 = new ArrayList<Float>();
		  	List ema2 = new ArrayList<Float>();
		  	List date = new ArrayList<String>();
		  	List volume = new ArrayList<Integer>();
		   while (rs.next()){
			   open.add(rs.getFloat("open"));
		  		high.add(rs.getFloat("high"));
		  		low.add(rs.getFloat("low"));
		  		close.add(rs.getFloat("close"));
		  		ema1.add(rs.getFloat("ema1"));
		  		ema2.add(rs.getFloat("ema2"));
		  		date.add(rs.getString("tradedate"));
		   }
		   int count=0;float percProfitAtClosePrice=0f;
		   for(int i=1; i< date.size(); i++){
			   count=0;
			   float diff = ((float)open.get(i)-(float)close.get(i))*100/(float)close.get(i);
			   if((float)open.get(i)>(float)ema1.get(i) && (float)close.get(i)>(float)ema2.get(i)
					   && diff> 3 && (float)close.get(i)<(float)ema1.get(i)){
				   for(int j=i-10; j< i; j++){
					   if((float)close.get(j) > ((float)ema2.get(j)) && (float)ema2.get(j) !=0){
							count++;
						}else count=0;
				   }
				   if(count > 8){
						count=0;
						percProfitAtClosePrice = ((float)open.get(i+1) - (float)low.get(i+1))*100/(float)open.get(i+1);
						sql = "insert into williamsresults(name, reversal, triggerPrice, profitPerc, profitRupees, date, dateexited) "
			  					+ " values ('"+name+"', 'Bear', "+percProfitAtClosePrice+", "+percProfitAtClosePrice+", "
			  							+ ""+diff+", '"+date.get(i+1)+"', '"+date.get(i+1)+"')";
						executeSqlQuery(con, sql);
					}
			   }
				
		   }
	   }
	   
	   
	   public void UpdatePinResultsRange2(java.sql.Connection con, String name,String dailyDate, float high, float low,String gapDir, int interval){
		   ResultSet rs=null;
		  	String sql="";
		  	List<Float> ema2 = new ArrayList<>();
		  	try {
		  		
			  	float highDay=0f, lowday=100000f;
			  	String tableName = "williamsresults";int count=0;
			  	float rangeLow, rangeHigh;
				 float profitBullPerc=0f;float range=0f, profitBearPerc=0f;
				 float entry=0f, exitPrice=0f;
				 if(gapDir.equals("lower")){
					 
					 sql = "select case when(open<low+(high-low)/3 and close<low+(high-low)/3 and close<open) then 'GoForBear' else '' end from "+name+"_"+interval+" where "+
							 "date(tradedate) = date('"+dailyDate+"') limit 1";
					 if(executeCountQuery(con, sql).equals("GoForBear")){
						 entry = Float.parseFloat(executeCountQuery(con, "select close from "+name+"_"+interval+" where date(tradedate) = date('"+dailyDate+"') limit 1"));
						 String entryDate = (executeCountQuery(con, "select tradedate from "+name+"_"+interval+" where date(tradedate) = date('"+dailyDate+"') limit 1"));
						 exitPrice = Float.parseFloat(executeCountQuery(con, "select min(low) from "+name+"_"+interval+" "
							 		+ " where date(tradedate)='"+dailyDate+"' and tradedate > '"+entryDate+"'"));
						 profitBearPerc = (entry-exitPrice)*100/entry;
			  				sql = "insert into "+tableName+"(name, reversal, triggerPrice, profitPerc, profitRupees, date, dateexited, noofcandles) "
		         					+ " values ('"+name+"', 'Bear2', 0, 0, "+profitBearPerc+", '"+dailyDate+"', '"+dailyDate+"', '1')";
		          			executeSqlQuery(con, sql);
					 }
				 }
				 
				 if(gapDir.equals("upper")){
					 sql = "select case when(open>high-(high-low)/3 and close>high-(high-low)/3 and close>open) then 'GoForBull' else '' end from "+name+"_"+interval+" where "+
							 "date(tradedate) = date('"+dailyDate+"') limit 1";
					 if(executeCountQuery(con, sql).equals("GoForBull")){
						 entry = Float.parseFloat(executeCountQuery(con, "select close from "+name+"_"+interval+" where date(tradedate) = date('"+dailyDate+"') limit 1"));
						 String entryDate = (executeCountQuery(con, "select tradedate from "+name+"_"+interval+" where date(tradedate) = date('"+dailyDate+"') limit 1"));
						 exitPrice = Float.parseFloat(executeCountQuery(con, "select max(high) from "+name+"_"+interval+" "
						 		+ " where date(tradedate)='"+dailyDate+"' and tradedate > '"+entryDate+"'"));
						 profitBullPerc = (exitPrice - entry)*100/entry;
			  				sql = "insert into "+tableName+"(name, reversal, triggerPrice, profitPerc, profitRupees, date, dateexited, noofcandles) "
		         					+ " values ('"+name+"', 'Bull2', 0, 0, "+profitBullPerc+", '"+dailyDate+"', '"+dailyDate+"', '1')";
		          			executeSqlQuery(con, sql);
					 }
				 }
				 
		  	}
		  	catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  	catch(Exception e){
		    	  e.printStackTrace();
		    }
	   }
}