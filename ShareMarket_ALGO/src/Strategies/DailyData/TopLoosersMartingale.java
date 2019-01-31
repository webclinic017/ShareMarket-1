package Strategies.DailyData;

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

public class TopLoosersMartingale extends Connection {
	public void getHighDeliveryPercDates(java.sql.Connection con, String name) {
		ResultSet rs = null;
		float percentage = 80f;
		List perc = new ArrayList<Float>();
		List tradedate = new ArrayList<String>();
		List tradedQuantity = new ArrayList<Long>();
		List open = new ArrayList<Float>();
		List high = new ArrayList<Float>();
		List low = new ArrayList<Float>();
		List close = new ArrayList<Float>();
		List tradedateF = new ArrayList<String>();
		List openF = new ArrayList<Float>();
		List highF = new ArrayList<Float>();
		List lowF = new ArrayList<Float>();
		List closeF = new ArrayList<Float>();
		List max_high = new ArrayList<Float>();
		TopLoosersMartingale gap = new TopLoosersMartingale();
		String date = "", sql = "";
		float percProfitAtHighPrice = 0f, percProfitAtClosePrice = 0f, percProfitAtLowPrice = 0f;
		try {
			// name= name+"_FUT";
			sql = "select * from  " + name + " as a where a.tradedate>='2016-01-01' ";
			rs = executeSelectSqlQuery(con, sql);
			while (rs.next()) {
				tradedate.add(rs.getString("a.tradedate"));
				open.add(rs.getFloat("a.open"));
				high.add(rs.getFloat("a.high"));
				low.add(rs.getFloat("a.low"));
				close.add(rs.getFloat("a.close"));
			}
			float trig = 0f, prev1, prev2, prev3, prev4, width;
			float gapPerc = 2f, gapLimitPerc = 5f, div = 4f;
			int iter = 1;
			String out = "";
			float stopLossPerc = 1f, targetPerc = 4f;
			for (int i = 1; i < tradedate.size(); i++) {
				float diff = 0f, stopLoss = 0f;
				if (((float) open.get(i) - (float) high.get(i - 1)) * 100 / (float) high.get(i - 1) > gapPerc
						&& ((float) open.get(i) - (float) high.get(i - 1)) * 100
								/ (float) high.get(i - 1) < gapLimitPerc) {
					float trigger = ((float) open.get(i) - (float) high.get(i - 1)) / div;
					trigger = ((float) open.get(i) - trigger);
					diff = (float) (trigger - (trigger * targetPerc / 100));
					stopLoss = (float) (trigger + (trigger * stopLossPerc / 100));
					if ((float) low.get(i) < trigger) {
						percProfitAtClosePrice = (trigger - (float) close.get(i)) * 100 / (float) trigger;
						percProfitAtLowPrice = (trigger - (float) low.get(i)) * 100 / (float) trigger;

						// if(percProfitAtLowPrice>targetPerc)
						// percProfitAtLowPrice = targetPerc;
						// else percProfitAtLowPrice = (trigger -
						// (float)close.get(i))*100/(float)trigger;
						sql = "insert into williamsresults(name, reversal, triggerPrice, profitPerc, profitRupees, date) "
								+ " values ('" + name + "', 'Bear', " + percProfitAtClosePrice + ", "
								+ percProfitAtLowPrice + ", " + diff + ", '" + tradedate.get(i) + "')";
						executeSqlQuery(con, sql);
					}
				}
				diff = (float) open.get(i) - (float) low.get(i);
				if (((float) low.get(i - 1) - (float) open.get(i)) * 100 / (float) open.get(i) > gapPerc
						&& ((float) low.get(i - 1) - (float) open.get(i)) * 100 / (float) open.get(i) < gapLimitPerc) {
					float trigger = ((float) low.get(i - 1) - (float) open.get(i)) / div;
					trigger = ((float) open.get(i) + trigger);
					diff = (float) (trigger + (trigger * targetPerc / 100));
					stopLoss = (float) (trigger - (trigger * stopLossPerc / 100));
					if ((float) high.get(i) > trigger) {
						percProfitAtClosePrice = ((float) close.get(i) - trigger) * 100 / (float) trigger;
						percProfitAtHighPrice = ((float) high.get(i) - trigger) * 100 / (float) trigger;

						// if(percProfitAtHighPrice>targetPerc)
						// percProfitAtHighPrice = targetPerc;
						// else percProfitAtHighPrice =
						// ((float)close.get(i)-trigger)*100/(float)trigger;
						sql = "insert into williamsresults(name, reversal, triggerPrice, profitPerc, profitRupees, date) "
								+ " values ('" + name + "', 'Bull', " + percProfitAtClosePrice + ", "
								+ percProfitAtHighPrice + ", " + diff + ", '" + tradedate.get(i) + "')";
						executeSqlQuery(con, sql);

					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String isOpenPriceTouched(java.sql.Connection con, String name, String date, String dir, float trig) {
		String sql = "";
		int interval = 5;
		String isOpenPriceTouched = "";
		if (dir.equalsIgnoreCase("Bull")) {
			sql = "select tradedate from " + name + "_" + interval + " where high>" + trig
					+ " order by tradedate limit 1";
			String trigDate = executeCountQuery(con, sql);
			sql = "select case when (low < (open-OPEN*0.5/100)) then 1 else 0 end from " + name + "_" + interval
					+ " where tradedate > '" + trigDate + "' and date(tradedate) = '" + date + "'";
			isOpenPriceTouched = executeCountQuery(con, sql);
		}
		return isOpenPriceTouched;
	}

	public static void main(String[] args) {
		Test t = new Test();
		TopLoosersMartingale pin = new TopLoosersMartingale();
		java.sql.Connection dbConnection = null;
		boolean updateSymbolsTableData = true;
		boolean updateAllData = true;
		try {

			Connection con = new Connection();
			dbConnection = con.getDbConnection();
			ResultSet rs = null;
			String sql = "";
			sql = "SELECT s.name FROM symbols s where nifty_50=1";
			rs = con.executeSelectSqlQuery(dbConnection, sql);
			String name = "";
			boolean updateForTodayAndNextDay = true;
			boolean updateForallDays = true;
			boolean updateResultTable = false;
			boolean isIntraDayData = false;
			boolean insertAllDataToResult = false;
			String iter = "1d";
			String path = "C:/Puneeth/SHARE_MARKET/Hist_Data/Intraday/";
			List<String> list = new ArrayList<>();
			List<String> dateList = new ArrayList<>();
			while (rs.next()) {
				name = rs.getString("s.name");
				list.add(name);
			}
			sql = "select tradedate from sbin where tradedate>='2018-12-13'";
			rs = con.executeSelectSqlQuery(dbConnection, sql);
			while (rs.next()) {
				dateList.add(rs.getString("tradedate"));
			}
			float pC, tC, maxLoss=100000;
			String maxLossSymbol="";
			for(int i=0; i< dateList.size(); i++){
				maxLoss=0;maxLossSymbol="";
				for(int j=0; j<list.size(); j++){
					try{
						pC = Float.parseFloat(con.executeCountQuery(dbConnection, "select close from `"+list.get(j)+"`"
								+ " where tradedate<'"+dateList.get(i)+"' order by tradedate desc limit 1 "));
						tC = Float.parseFloat(con.executeCountQuery(dbConnection, "select close from `"+list.get(j)+"`"
								+ " where tradedate='"+dateList.get(i)+"' order by tradedate desc limit 1 "));
						if((pC-tC)*100/pC >maxLoss && tC<pC){
							maxLossSymbol=list.get(j);
							maxLoss = (pC-tC)*100/pC;
							System.out.println(dateList.get(i)+","+maxLoss+","+maxLossSymbol);
						}
					}catch(Exception e){}
				}
				
			}
		}

		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dbConnection != null)
				try {
					dbConnection.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
}