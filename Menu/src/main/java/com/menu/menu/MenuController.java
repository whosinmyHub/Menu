package com.menu.menu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

	private static Connection conn;

	public MenuController() {

		try {
			conn = DriverManager.getConnection("jdbc:h2:mem:menudb", "sa", "");

			if (conn == null)
				throw new SQLException();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * getMenu () is a GET endpoint for retrieving the menu
	 * 
	 * @return the list of things on the menu or null
	 */
	@GetMapping("/getMenu")
	public String getMenu() {
		try (Statement stmt = conn.createStatement()) {

			ResultSet menu = stmt.executeQuery("SELECT * FROM Menu");
			StringBuilder menuString = new StringBuilder();

			while (menu.next()) {
				menuString.append(menu.getString(1) + " " + menu.getString(2) + "\n");
			}

			return menuString.toString();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * getOrder () is a GET endpoint for retrieving the all orders placed
	 * 
	 * @return the list of things ordered or null
	 */
	@GetMapping("/getOrders")
	public String getOrders() {

		try (Statement stmt = conn.createStatement()) {

			ResultSet orders = stmt.executeQuery("SELECT * FROM Orders ORDER BY table_id");

			StringBuilder ordersString = new StringBuilder();
			ordersString.append("table_id\tfood_name\tquantity\n");

			while (orders.next()) {
				ordersString.append(orders.getInt(1) + "\t\t" + orders.getString(2) + "\t" + orders.getInt(3) + "\n");
			}

			return ordersString.toString();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * seatTable () is a POST endpoint for inserting a new table id into the DB
	 * 
	 * @return the new table id or null
	 */
	@PostMapping("/seatTable")
	public String seatTable() {
		try {

			Statement stmt = conn.createStatement();

			int numRowsInserted = stmt.executeUpdate("INSERT INTO TABLES VALUES (DEFAULT)");

			if (numRowsInserted > 0) {
				ResultSet table_ids = stmt.executeQuery("SELECT table_id FROM TABLES ORDER BY table_id DESC LIMIT 1");

				if (table_ids.next()) {
					int id = table_ids.getInt(1);
					return "Your table id is " + id;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * postOrder () is a POST endpoint for inserting a new order into the DB
	 * 
	 * @return the order or an error message or null
	 */
	@PostMapping("/postOrder")
	public String postOrder(@RequestBody OrderRecord order) {

		/////////////////////////////////////////
		// error checking if the food ordered is in the menu & if the table id exists
		try {
			Statement existsStmt = conn.createStatement();

			ResultSet foodExists = existsStmt
					.executeQuery("SELECT * FROM Menu WHERE food_name = '" + order.food_name() + "'");
			if (!foodExists.next())
				return order.food_name() + " is not on the menu. Please order something from the menu.";

			ResultSet tableExists = existsStmt
					.executeQuery("SELECT * FROM Tables WHERE table_id = " + order.table_id());
			if (!tableExists.next())
				return "Table " + order.table_id() + " has not been seated yet";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/////////////////////////////////////////

		// a quantity doesn't have to be specified, so adjust the query accordingly
		String insertOrder;
		boolean hasQuantity = order.quantity() != null;

		if (hasQuantity)
			insertOrder = "INSERT INTO Orders " + "VALUES ( ?, ?, ?)";
		else
			insertOrder = "INSERT INTO Orders " + "VALUES ( ?, ?, DEFAULT)";

		// use of PreparedStatement over Statement helps protect against SQL injection
		// attacks
		try (PreparedStatement pStmt = conn.prepareStatement(insertOrder)) {
			conn.setAutoCommit(false);

			pStmt.setInt(1, order.table_id());
			pStmt.setString(2, order.food_name());

			if (hasQuantity)
				pStmt.setInt(3, order.quantity());

			pStmt.executeUpdate();
			conn.commit();

			ResultSet orders = conn.createStatement()
					.executeQuery("SELECT * FROM Orders WHERE table_id = " + order.table_id());
			StringBuilder table_ordered = new StringBuilder("Table " + order.table_id() + " has ordered :\n\t");

			while (orders.next()) {
				table_ordered.append(orders.getInt(3) + " " + orders.getString(2) + "\n\t");
			}

			return table_ordered.toString();

		} catch (SQLException e) {
			e.printStackTrace();

			if (conn != null) {

				try {

					System.err.print("Transaction is being rolled back.");
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
		}

		return null;

	}

	/**
	 * postOrder () is a POST endpoint for inserting a multiple new orders into the
	 * DB
	 * 
	 * @return the orders or an error message or null
	 */
	@PostMapping("/postOrders")
	public String postOrder(@RequestBody List<OrderRecord> orders) {

		StringBuilder allOrders = new StringBuilder();

		for (OrderRecord order : orders) {

			/////////////////////////////////////////
			// error checking if the food ordered is in the menu & if the table id exists
			try {
				Statement existsStmt = conn.createStatement();

				ResultSet foodExists = existsStmt
						.executeQuery("SELECT * FROM Menu WHERE food_name = '" + order.food_name() + "'");
				if (!foodExists.next())
					return order.food_name() + " is not on the menu. Please order something from the menu.";

				ResultSet tableExists = existsStmt
						.executeQuery("SELECT * FROM Tables WHERE table_id = " + order.table_id());
				if (!tableExists.next())
					return "Table " + order.table_id() + " has not been seated yet";

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/////////////////////////////////////////

			// a quantity doesn't have to be specified, so adjust the query accordingly
			String insertOrder;
			boolean hasQuantity = order.quantity() != null;

			if (hasQuantity)
				insertOrder = "INSERT INTO Orders " + "VALUES ( ?, ?, ?)";
			else
				insertOrder = "INSERT INTO Orders " + "VALUES ( ?, ?, DEFAULT)";

			// use of PreparedStatement over Statement helps protect against SQL injection
			// attacks
			try (PreparedStatement pStmt = conn.prepareStatement(insertOrder)) {
				conn.setAutoCommit(false);

				pStmt.setInt(1, order.table_id());
				pStmt.setString(2, order.food_name());

				if (hasQuantity)
					pStmt.setInt(3, order.quantity());

				pStmt.executeUpdate();
				conn.commit();

				ResultSet ordersSet = conn.createStatement()
						.executeQuery("SELECT * FROM Orders WHERE table_id = " + order.table_id());
				StringBuilder table_ordered = new StringBuilder("Table " + order.table_id() + " has ordered :\n\t");

				while (ordersSet.next()) {
					table_ordered.append(ordersSet.getInt(3) + " " + ordersSet.getString(2) + "\n\t");
				}

				allOrders.append(table_ordered.toString() + "\n");

			} catch (SQLException e) {
				e.printStackTrace();

				if (conn != null) {

					try {

						System.err.print("Transaction is being rolled back.");
						conn.rollback();
					} catch (SQLException ex) {
						ex.printStackTrace();
					}
				}
			}

		}

		return allOrders.toString();
	}

	/**
	 * updateOrder () is a PUT endpoint for updating an existing order in the DB
	 * 
	 * @return the updated order transaction or an error message or null
	 */
	@PutMapping("/updateOrder")
	public String updateOrder(@RequestBody OrderRecord updatedOrder) {

		/////////////////////////////////////////
		// error checking if table id already ordered this food
		try {
			Statement existsStmt = conn.createStatement();

			ResultSet orderExists = existsStmt.executeQuery("SELECT * FROM Orders WHERE table_id = "
					+ updatedOrder.table_id() + " AND food_name = '" + updatedOrder.food_name() + "'");
			if (!orderExists.next())
				return "Table " + updatedOrder.table_id() + " must order " + updatedOrder.food_name()
						+ " before updating it";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/////////////////////////////////////////

		try {

			ResultSet prevOrder = conn.createStatement().executeQuery("SELECT * FROM Orders WHERE table_id = "
					+ updatedOrder.table_id() + " AND food_name = '" + updatedOrder.food_name() + "'");

			if (prevOrder.next()) {

				PreparedStatement pStmt = conn
						.prepareStatement("UPDATE ORDERS SET quantity = ? WHERE table_id = ? AND food_name = ?");

				conn.setAutoCommit(false);

				pStmt.setInt(1, updatedOrder.quantity());
				pStmt.setInt(2, prevOrder.getInt(1));
				pStmt.setString(3, prevOrder.getString(2));

				pStmt.executeUpdate();
				conn.commit();

				ResultSet selectOrderQuery = conn.createStatement()
						.executeQuery("SELECT * FROM Orders WHERE table_id = " + updatedOrder.table_id()
								+ " AND food_name = '" + updatedOrder.food_name() + "'");

				StringBuilder update = new StringBuilder("Table " + updatedOrder.table_id() + "'s order of "
						+ prevOrder.getInt(3) + " " + prevOrder.getString(2) + " has been updated to ");

				if (selectOrderQuery.next()) {
					update.append(selectOrderQuery.getInt(3) + " " + selectOrderQuery.getString(2));
				}

				return update.toString();

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * updateOrder () is a PUT endpoint for updating existing orders in the DB
	 * 
	 * @return the updated order transactions or an error message or null
	 */
	@PutMapping("/updateOrders")
	public String updateOrder(@RequestBody List<OrderRecord> updatedOrders) {

		StringBuilder updatedOrdersString = new StringBuilder();

		for (OrderRecord updatedOrder : updatedOrders) {

			/////////////////////////////////////////
			// error checking if table id already ordered this food
			try {
				Statement existsStmt = conn.createStatement();

				ResultSet orderExists = existsStmt.executeQuery("SELECT * FROM Orders WHERE table_id = "
						+ updatedOrder.table_id() + " AND food_name = '" + updatedOrder.food_name() + "'");
				if (!orderExists.next())
					return "Table " + updatedOrder.table_id() + " must order " + updatedOrder.food_name()
							+ " before updating it";

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/////////////////////////////////////////

			try {

				ResultSet prevOrder = conn.createStatement().executeQuery("SELECT * FROM Orders WHERE table_id = "
						+ updatedOrder.table_id() + " AND food_name = '" + updatedOrder.food_name() + "'");

				if (prevOrder.next()) {

					PreparedStatement pStmt = conn
							.prepareStatement("UPDATE ORDERS SET quantity = ? WHERE table_id = ? AND food_name = ?");

					conn.setAutoCommit(false);

					pStmt.setInt(1, updatedOrder.quantity());
					pStmt.setInt(2, prevOrder.getInt(1));
					pStmt.setString(3, prevOrder.getString(2));

					pStmt.executeUpdate();
					conn.commit();

					ResultSet selectOrderQuery = conn.createStatement()
							.executeQuery("SELECT * FROM Orders WHERE table_id = " + updatedOrder.table_id()
									+ " AND food_name = '" + updatedOrder.food_name() + "'");

					StringBuilder update = new StringBuilder("Table " + updatedOrder.table_id() + "'s order of "
							+ prevOrder.getInt(3) + " " + prevOrder.getString(2) + " has been updated to ");

					if (selectOrderQuery.next()) {
						update.append(selectOrderQuery.getInt(3) + " " + selectOrderQuery.getString(2));
					}

					updatedOrdersString.append(update.toString() + "\n");

				}

			} catch (SQLException e) {
				e.printStackTrace();
			}

		}

		return updatedOrdersString.toString();
	}

	/**
	 * deleteOrder () is a DELETE endpoint for deleting an existing order in the DB
	 * 
	 * @return the deleted order transaction or an error message or null
	 */
	@DeleteMapping("deleteOrder")
	public String deleteOrder(@RequestBody OrderRecord orderToDelete) {

		/////////////////////////////////////////
		// error checking if table id already ordered this food
		try {
			Statement existsStmt = conn.createStatement();

			ResultSet orderExists = existsStmt.executeQuery("SELECT * FROM Orders WHERE table_id = "
					+ orderToDelete.table_id() + " AND food_name = '" + orderToDelete.food_name() + "'");
			if (!orderExists.next())
				return "Table " + orderToDelete.table_id() + " must order " + orderToDelete.food_name()
						+ " before deleting it";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/////////////////////////////////////////

		try {

			PreparedStatement pStmt;
			conn.setAutoCommit(false);

			if (orderToDelete.quantity() != null) {
				pStmt = conn
						.prepareStatement("DELETE FROM Orders WHERE table_id = ? AND food_name = ? AND quantity = ?");

				pStmt.setInt(1, orderToDelete.table_id());
				pStmt.setString(2, orderToDelete.food_name());
				pStmt.setInt(3, orderToDelete.quantity());

			}

			else {
				pStmt = conn.prepareStatement("DELETE FROM Orders WHERE table_id = ? AND food_name = ?");

				pStmt.setInt(1, orderToDelete.table_id());
				pStmt.setString(2, orderToDelete.food_name());
			}

			pStmt.executeUpdate();
			conn.commit();

			int quantity = orderToDelete.quantity() == null ? 1 : orderToDelete.quantity();
			return "Table " + orderToDelete.table_id() + "'s order of " + quantity + " " + orderToDelete.food_name()
					+ " has been deleted";

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * deleteOrder () is a DELETE endpoint for deleting existing orders in the DB
	 * 
	 * @return the deleted order transactions or an error message or null
	 */
	@DeleteMapping("deleteOrders")
	public String deleteOrder(@RequestBody List<OrderRecord> ordersToDelete) {

		StringBuilder msgs = new StringBuilder();

		for (OrderRecord orderToDelete : ordersToDelete) {

			/////////////////////////////////////////
			// error checking if table id already ordered this food
			try {
				Statement existsStmt = conn.createStatement();

				ResultSet orderExists = existsStmt.executeQuery("SELECT * FROM Orders WHERE table_id = "
						+ orderToDelete.table_id() + " AND food_name = '" + orderToDelete.food_name() + "'");
				if (!orderExists.next())
					return "Table " + orderToDelete.table_id() + " must order " + orderToDelete.food_name()
							+ " before deleting it";

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/////////////////////////////////////////

			try {

				PreparedStatement pStmt;
				conn.setAutoCommit(false);

				if (orderToDelete.quantity() != null) {
					pStmt = conn.prepareStatement(
							"DELETE FROM Orders WHERE table_id = ? AND food_name = ? AND quantity = ?");

					pStmt.setInt(1, orderToDelete.table_id());
					pStmt.setString(2, orderToDelete.food_name());
					pStmt.setInt(3, orderToDelete.quantity());

				}

				else {
					pStmt = conn.prepareStatement("DELETE FROM Orders WHERE table_id = ? AND food_name = ?");

					pStmt.setInt(1, orderToDelete.table_id());
					pStmt.setString(2, orderToDelete.food_name());
				}

				pStmt.executeUpdate();
				conn.commit();

				int quantity = orderToDelete.quantity() == null ? 1 : orderToDelete.quantity();
				msgs.append("Table " + orderToDelete.table_id() + "'s order of " + quantity + " "
						+ orderToDelete.food_name() + " has been deleted\n");

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return msgs.toString();
	}

}
