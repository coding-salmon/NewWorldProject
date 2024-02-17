package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SalesDatabase {
	private Connection connection;
	
	//데이터베이스 연결 메소드
	public void connect() {
		try {
			//JDBC 드라이버 클래스 로드
			Class.forName("oracle.jdbc.driver.OracleDriver");
			
			//데이터베이스 연결 정보 설정
			String url ="jdbc:oracle:thin:@localhost:1521:XE";
			String username ="##salmon";
			String password ="1234";
			
			//데이터베이스 연결
			connection = DriverManager.getConnection(url, username, password);
			
			System.out.println("데이터베이스 연결 성공!");
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}
	public void close() {
		try {
			if(connection !=null && !connection.isClosed()) {
				connection.close();
				System.out.println("데이터베이스 연결 종료");
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	public void saveSales(String id, String coffeeName, int quantity, double totalPrice) {
		//데이터 베이스 연결 확인
		if(connection == null) {
			System.out.println("데이터베이스 연결을 확인하세요");
			return;
		}
		String sql ="INSERT INTO sales (user_id, coffee_name, quantity, total_prcie) VALUES(?,?,?,?)";
		
		try(PreparedStatement pstmt = connection.prepareStatement(sql)){
			pstmt.setString(1, id);
			pstmt.setString(2, coffeeName);
			pstmt.setInt(3, quantity);
			pstmt.setDouble(4, totalPrice);
			
			int affectedRows = pstmt.executeUpdate();
			
			if(affectedRows>0) {
				System.out.println("판매 정보 저장 성공");
			}else {
				System.out.println("판매 정보 저장 실패");
			}
		}catch(SQLException e) {
			System.out.println("판매 정보 저장 중 오류 발생: " +e.getMessage());
		}
	}

}
