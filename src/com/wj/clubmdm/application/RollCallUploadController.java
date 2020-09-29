/* 
 * Colors Sports Club 點名資料上傳 Controller
 * 
 * @author 黃郁授,吳彥儒
 * @date 2020/09/22
 */

package com.wj.clubmdm.application;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;


import com.wj.clubmdm.vo.RollCallDetail;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import rhinoceros.util.db.DBConnectionFactory;
import rhinoceros.util.file.UTF8FileReader;


public class RollCallUploadController extends Application {
	
	private Logger logger = Logger.getLogger(RollCallUploadController.class);
	//暫存點名資料用
	private TreeMap<String, RollCallDetail> rollCallDetails = null;
	
	@FXML
	private DatePicker dpChoiceRollCallDate; //選擇點名日期
	@FXML
	private TextField tfRollCallDate; //點名日期
	@FXML
	private TextField tfFilePath; //點名檔絕對路徑
	@FXML
	private Button btnChoiceRollCallFile; //選擇點名檔
	@FXML
	private Button btnCheckData; //檢查檔案資料
	@FXML
	private TableView<RollCallDetail> tvRollCallDetail; //點名資料
	@FXML
	private TableColumn<RollCallDetail, String> colStudentNo; //點名資料_學員編號
	@FXML
	private TableColumn<RollCallDetail, String> colName; //點名資料_姓名
	@FXML
	private TableColumn<RollCallDetail, String> colDepartment; //點名資料_上課分部
	@FXML
	private TableColumn<RollCallDetail, String> colCourseKind; //點名資料_類別
	@FXML
	private TableColumn<RollCallDetail, String> colLevel; //點名資料_程度
	@FXML
	private TableColumn<RollCallDetail, String> colRollCallDate; //點名資料_點名日期	
	@FXML
	private TableColumn<RollCallDetail, String> colRollSpecial; //點名資料_特色課程	

	
	/*
	 * 初始化
	 */
	public void initialize() {
		//設定日期選擇器的格式
		StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			@Override public String toString(LocalDate date) {
				if(date !=null ) {
					return formatter.format(date);
				} else {
					return "";
				}
			}
			
			@Override public LocalDate fromString(String string) {
				if (string != null && !string.isEmpty()) {
					return LocalDate.parse(string, formatter);
				} else {
					return null;
				}
			}
		};
		dpChoiceRollCallDate.setConverter(converter);		
	}

	/*
	 * 將Excel檔案吃入
	 */
	public void checkRollCallFile() {
		//提示錯誤的對話框
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("資料輸入有誤");
		
		chkField(); //檢核欄位值
		//檢核Excel檔案是否存在
		File file = new File(tfFilePath.getText().trim());
		if (!file.exists()) {
			alert.setContentText("點名檔不存在，請確認檔案路徑！");
			alert.showAndWait();
			return;		
		}
		//建立用來存TSV檔裡面內容的暫存變數
		ArrayList<String> data = null; 
		//建立之後要給點名資料ViewTable用的暫存變數
		rollCallDetails = new TreeMap<String, RollCallDetail>();
		//將檔案內容讀出
		UTF8FileReader u8r = new UTF8FileReader();
		try {
			data = u8r.replace(tfFilePath.getText().trim(), "\\s+", "|", true);
			RollCallDetail rcd = null;
			//逐一整理
			for (String s : data) {
				rcd = new RollCallDetail();
				if (rcd != null) {
					//加入rollCallDetails
				}
			}
		} catch (Exception e) {
			logger.info(e.getMessage(), e);
		}
		
	}
	
	/*
	 * 將 A000002|2020/9/24|上午|12:01:01 轉成 RollCallDetail 物件後回傳
	 */
	private RollCallDetail turnIntoRollCallDetail(String rawData) {
		RollCallDetail rcd = new RollCallDetail();
		String[] arrayData = rawData.split("|");
		//第1個欄位的值的格式必須是 A + 6碼流水號，才處理
		if (arrayData[0].substring(0, 1).equalsIgnoreCase("A") && arrayData[0].length() == 7) {
			rcd.setStudentNo(arrayData[0]);
		} else {
			logger.info("點名檔員編格式不正確 [" + rawData + "]");
			return null;
		}
		
		//轉換日期格式 由 2020/9/4 轉成 2020-09-04
		String[] tempDate = arrayData[1].split("/");
		
		//年份欄位若不是4碼代表資料有問題
		if (tempDate[0].length() != 4) {
			logger.info("點名檔年份不正確 [" + rawData + "]");
			return null;
		}
		
		//月份左邊補0
		if (tempDate[1].length() <= 1) {
			tempDate[1] = "0" + tempDate[1];
		}
		
		//日期左邊補0
		if (tempDate[2].length() <= 1) {
			tempDate[2] = "0" + tempDate[2];
		}
		arrayData[1] = tempDate[0] + "-" + tempDate[1] + "-" + tempDate[2];
		
		//第3個欄位欄位必須是 上午 or 下午 這兩個字樣
		Integer adjHours = null;
		if (arrayData[2].equalsIgnoreCase("上午")) {
			adjHours = -12;
		} else if (arrayData[2].equalsIgnoreCase("下午")) {
			adjHours = 0;
		} else {
			logger.info("點名檔時間格式有誤，缺少上午、下午字樣 [" + rawData + "]");
			return null;
		}
		
		//第4個欄位為HH:MM:SS
		if (arrayData[3].length() != 8) {
			return null;
		} else {
			//把小時的部份改為24小時制
			String[] tempTime = arrayData[3].split(":");
			Integer hour = null;
			try {
				hour = Integer.parseInt(tempTime[0]) + adjHours;
				if (hour >= 13 || hour == 0) {
					logger.info("點名檔時間格式有誤 上午為12:00-11:59 下午為12:00-11:59 [" + rawData + "]");					
				}
				tempTime[0] = hour.toString();
				arrayData[3] = tempTime[0] + tempTime[1] + tempTime[2];
			} catch(Exception e) {
				logger.info("點名檔時間格式有誤 " + rawData);
				return null;				
			}
		}
		
		//到這邊代表時間格式正確，將其組成 yyyy-mm-dd hh(24小時制):mm:ss
		rcd.setRollCallTime(arrayData[1] + " " + arrayData[3]);

		//取得學員相關資料(★寫到這邊)
		/*
		String sql = 
				"SELECT " + 
				"	f.name," + 
				"	(select d.desc from Student s left join CodeDetail d on s.Sex = d.DetailCode and d.MainCode = '001') SexDesc," + 
				"	(select d.desc from Student s left join CodeDetail d on s.Department = d.DetailCode and d.MainCode = '004') DepartmentDesc," + 
				"	(select d.desc from Student s left join CodeDetail d on s.Level = d.DetailCode and d.MainCode = '005') LevelDesc " + 
				"FROM " + 
				"  Student f " + 
				"where" + 
				"  f.StudentNo = ?";
		DBConnectionFactory dbf = new DBConnectionFactory();
		Connection conn = null;
		try {
			
			conn = dbf.getSQLiteCon("", "Club.dll");
			if (update) {
				sql = "update Note set Note = ? where key = 'Stock'";
			} else {
				sql = "insert into Note values( '1', ?)";		
			}
			pstmt = conn.prepareStatement(sql);
			pstmt.clearParameters();
			pstmt.setString(1, taStrategyNote.getText());
			pstmt.executeUpdate();	
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		*/
		
		
		return rcd;
	}
	
	
	/*
	 * 檢查輸入欄位
	 */
	private boolean chkField() {
		//提示錯誤的對話框
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setHeaderText("資料輸入有誤");
				
		//檢核日期是否正確
		if (dpChoiceRollCallDate.getValue() == null) {			
			alert.setContentText("點名日期不正確！");
			alert.showAndWait();
			return false;			
		}
		return true;
	}
	
	/*
	 * 選擇點名檔(.tsv)
	 */
	public void choiceFile() {
		FileChooser filechooser = new FileChooser();
		filechooser.setTitle("選擇 點名檔(*.tsv)...");
		filechooser.setInitialDirectory(new File("."));
		FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("TSV", "*.tsv");
		filechooser.getExtensionFilters().add(filter);
		/*
		 * 跳出對話框時，有兩種方法取得獨佔視窗的對話框
		 * (1)透過button去取得Stage，這樣的效果會是獨佔模式
		 * (2)上一層在建立controller時，把stage當成參數傳入給controller的屬性，這樣可以直接透過屬性去取得stage
		 */
		File file = filechooser.showOpenDialog((Stage) btnChoiceRollCallFile.getScene().getWindow());
		if (file != null) {
			tfFilePath.setText(file.getAbsolutePath());			
		} else {
			tfFilePath.setText("");						
		}
	}
	
	
	@Override
	public void start(Stage arg0) throws Exception {
		
	}
	
}
