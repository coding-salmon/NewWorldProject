package Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CoffeeDatabase {
	private Connection connection;
	
	public CoffeeDatabase() {
		connect();
	}
	
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
			
			System.out.println("커피 정보 데이터베이스 연결 성공!");
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}
	public void close() {
		try {
			if(connection !=null && !connection.isClosed()) {
				connection.close();
				System.out.println("커피 정보 데이터베이스 연결 종료");
			}
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public String getCoffeeInfo(int coffeeId) {
		String coffeeInfo="";
		try {
			String query = "SELECT*FROM Coffee WHERE coffee_id = ?";
			PreparedStatement statement = connection.prepareStatement(query);
			statement.setInt(1, coffeeId);
			ResultSet resultSet = statement.executeQuery();
			if(resultSet.next()) {
				String name = resultSet.getString("name");
				double price = resultSet.getDouble("price");
			}else {
				coffeeInfo ="coffee ID: " +coffeeId + " not found.";
			}
			resultSet.close();
			statement.close();
		}catch(SQLException e) {
			e.printStackTrace();
			coffeeInfo = "커피 정보를 검색하는 중 오류가 발생했습니다.";
		}
		return coffeeInfo;
	}

}
