package me.group.cceproject.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class AdminMainController {

    @FXML
    private TextField inputOrderNumber;

    @FXML
    private TextField OrderNumberInput;

    @FXML
    private TextField AmountTextField;

    @FXML
    private Text TotalText;

    @FXML
    private Text orderTotalText;

    @FXML
    private Text ChangeText;

    @FXML
    private ComboBox<String> orderStatusComboBox;

    @FXML
    private TabPane MainTab;

    @FXML
    private TabPane InputTab;

    @FXML
    private Tab OrderQueueTab;

    @FXML
    private Tab OrdersTab;

    @FXML
    private Tab OrderDetails;

    @FXML
    private Tab OrderInput;
    @FXML
    private TextField QuantityField;
    @FXML
    private TableView<OrderSummary> orderTableView;
    @FXML
    private TableColumn<OrderSummary, String> orderNumberColumn;
    @FXML
    private TableColumn<OrderSummary, String> orderTotalColumn;
    @FXML
    private TableColumn<OrderSummary, String> orderStatusColumn;
    @FXML
    private ComboBox<String> ProductIDBox;
    @FXML
    private TableView<OrderItem> OrdersTable;
    @FXML
    private TableColumn<OrderItem, String> ProductID;
    @FXML
    private TableColumn<OrderItem, String> OrderName;
    @FXML
    private TableColumn<OrderItem, String> OrderPrice;
    @FXML
    private TableColumn<OrderItem, Integer> OrderQuantity;

    private static final String ORDER_FILE = "orders.txt";

    // Queue for order processing
    private Queue<OrderSummary> orderQueue = new LinkedList<>();

    // Map to store stacks of OrderItems for each order
    private Map<String, Stack<OrderItem>> orderStacks = new HashMap<>();

    @FXML
    public void initialize() {
        // Set up the order summary columns
        orderNumberColumn.setCellValueFactory(cellData -> cellData.getValue().orderNumberProperty());
        orderTotalColumn.setCellValueFactory(cellData -> cellData.getValue().orderTotalProperty());
        orderStatusColumn.setCellValueFactory(cellData -> cellData.getValue().orderStatusProperty());

        // Set up the order details columns
        ProductID.setCellValueFactory(cellData -> cellData.getValue().foodCodeProperty());
        OrderName.setCellValueFactory(cellData -> cellData.getValue().mealNameProperty());
        OrderPrice.setCellValueFactory(cellData -> cellData.getValue().mealPriceProperty());
        OrderQuantity.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());

        orderStatusComboBox.getItems().addAll("Pending", "In Progress", "Completed");
        OrderNumberInput.setOnAction(event -> handleOrderNumberInput());
// Example product IDs and prices for demonstration
        productPrices.put("P001", 100.0);
        productPrices.put("P002", 150.0);
        productPrices.put("P003", 200.0);

        // Populate the ComboBox with product IDs
        ObservableList<String> productIds = FXCollections.observableArrayList(productPrices.keySet());
        ProductIDBox.setItems(productIds);
        inputOrderNumber.setOnAction(event -> loadOrderDetails());
        OrdersTable.getItems().addListener((ListChangeListener<OrderItem>) change -> updateTotalPrice());

        // Update the total price on startup
        updateTotalPrice();
        loadOrders();
    }

    private void loadOrders() {
        ObservableList<OrderSummary> orders = FXCollections.observableArrayList();
        try {
            List<String> lines = Files.readAllLines(Paths.get(ORDER_FILE));
            OrderSummary currentOrder = null;
            Stack<OrderItem> currentStack = null;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.matches("\\d{4}: Order Items")) {
                    if (currentOrder != null && currentStack != null) {
                        orders.add(currentOrder);
                        orderQueue.offer(currentOrder);  // Add to queue
                        orderStacks.put(currentOrder.getOrderNumber(), currentStack);
                    }
                    String orderNumber = line.split(":")[0];
                    currentOrder = new OrderSummary(orderNumber);
                    currentStack = new Stack<>();
                } else if (line.startsWith("Food Code:")) {
                    OrderItem item = new OrderItem("", "", line.substring(10).trim(), 0);
                    if (currentStack != null) {
                        currentStack.push(item);  // Add items to stack
                    }
                } else if (line.startsWith("Meal Name:") && !currentStack.isEmpty()) {
                    currentStack.peek().setMealName(line.substring(10).trim());
                } else if (line.startsWith("Price:") && !currentStack.isEmpty()) {
                    currentStack.peek().setMealPrice(line.substring(6).trim());
                } else if (line.startsWith("Quantity:") && !currentStack.isEmpty()) {
                    currentStack.peek().setQuantity(Integer.parseInt(line.substring(9).trim()));
                } else if (line.startsWith("Total Price:") && currentOrder != null) {
                    currentOrder.setOrderTotal(line.substring(12).trim());
                } else if (line.startsWith("Status:") && currentOrder != null) {
                    currentOrder.setOrderStatus(line.substring(7).trim());
                }
            }

            // Add the last order
            if (currentOrder != null && currentStack != null) {
                orders.add(currentOrder);
                orderQueue.offer(currentOrder);
                orderStacks.put(currentOrder.getOrderNumber(), currentStack);
            }

            orderTableView.setItems(orders);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load orders: " + e.getMessage());
        }
    }

    private void loadOrderDetails() {
        String orderNumberInput = inputOrderNumber.getText().trim();  // Get the entered order number
        if (orderNumberInput.isEmpty()) {
            showAlert("Error", "Please enter an order number");
            return;
        }

        // Loop through the orders in the orderTableView to find the matching order
        for (OrderSummary order : orderTableView.getItems()) {
            if (order.getOrderNumber().equals(orderNumberInput)) {
                // Update the orderTotalText with the total price of the selected order
                orderTotalText.setText(String.format("₱%.2f", Double.parseDouble(order.getOrderTotal())));

                // Update the orderStatusComboBox with the current status of the order
                orderStatusComboBox.setValue(order.getOrderStatus());
                return;  // Exit after updating the UI with the found order's details
            }
        }

        // If no matching order is found
        showAlert("Information", "Order number " + orderNumberInput + " not found.");
    }

    // This method will be called when the "Update Status" button is clicked
    @FXML
    private void UpdateStatusClicked(MouseEvent event) {
        String orderNumberInput = inputOrderNumber.getText();
        String newStatus = orderStatusComboBox.getValue();

        if (orderNumberInput.isEmpty() || newStatus == null) {
            showAlert("Error", "Order number or new status not selected");
            return;
        }

        for (OrderSummary order : orderTableView.getItems()) {
            if (order.getOrderNumber().equals(orderNumberInput)) {
                order.setOrderStatus(newStatus);

                if (newStatus.equals("Completed")) {
                    orderQueue.remove(order);  // Remove from queue
                    orderStacks.remove(order.getOrderNumber());  // Remove the stack
                    orderTableView.getItems().remove(order);
                }

                orderTableView.refresh();
                saveOrdersToFile();
                return;
            }
        }

        showAlert("Error", "Order number " + orderNumberInput + " not found");
    }

    private void saveOrdersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ORDER_FILE))) {
            for (OrderSummary order : orderTableView.getItems()) {
                writer.write(order.getOrderNumber() + ": Order Items\n");

                Stack<OrderItem> orderStack = orderStacks.get(order.getOrderNumber());
                if (orderStack != null) {
                    // Create temporary stack to preserve original order
                    Stack<OrderItem> tempStack = new Stack<>();
                    Stack<OrderItem> originalStack = new Stack<>();

                    // Copy items to temp stack (reverses order)
                    while (!orderStack.isEmpty()) {
                        tempStack.push(orderStack.pop());
                    }

                    // Write items and restore original stack
                    while (!tempStack.isEmpty()) {
                        OrderItem item = tempStack.pop();
                        writer.write("  Food Code: " + item.getFoodCode() + "\n");
                        writer.write("  Meal Name: " + item.getMealName() + "\n");
                        writer.write("  Price: " + item.getMealPrice() + "\n");
                        writer.write("  Quantity: " + item.getQuantity() + "\n");
                        orderStack.push(item);
                        originalStack.push(item);
                    }

                    // Restore original stack order
                    orderStack.clear();
                    while (!originalStack.isEmpty()) {
                        orderStack.push(originalStack.pop());
                    }
                }

                writer.write("  Total Price: " + order.getOrderTotal() + "\n");
                writer.write("  Status: " + order.getOrderStatus() + "\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save orders: " + e.getMessage());
        }
    }

    @FXML
    private void handleOrderNumberInput() {
        String orderNumber = OrderNumberInput.getText().trim();

        if (orderNumber.isEmpty()) {
            showAlert("Error", "Please enter an order number");
            return;
        }

        Stack<OrderItem> orderStack = orderStacks.get(orderNumber);
        if (orderStack != null && OrdersTable != null) {
            // Convert stack to ObservableList while maintaining order
            ObservableList<OrderItem> orderItems = FXCollections.observableArrayList();
            Stack<OrderItem> tempStack = new Stack<>();

            // Reverse the stack to display items in original order
            while (!orderStack.isEmpty()) {
                tempStack.push(orderStack.pop());
            }

            // Add items to observable list and restore the original stack
            while (!tempStack.isEmpty()) {
                OrderItem item = tempStack.pop();
                orderItems.add(item);
                orderStack.push(item);
            }

            OrdersTable.setItems(orderItems);

            // Now that we've loaded the order items into the OrdersTable, update the total price
            updateTotalPrice();
        } else {
            showAlert("Information", "No items found for order number: " + orderNumber);
        }
    }


    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private Map<String, Double> productPrices = new HashMap<>();
    @FXML
    private void AddClicked(MouseEvent event) {
        String selectedProductID = ProductIDBox.getSelectionModel().getSelectedItem();
        if (selectedProductID == null) {
            showAlert("Error", "Please select a product");
            return;
        }

        Double productPrice = productPrices.get(selectedProductID);  // Retrieve the actual price
        int productQuantity;

        try {
            productQuantity = Integer.parseInt(QuantityField.getText());
        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number for quantity");
            return;
        }

        OrderItem newItem = new OrderItem(selectedProductID, "Product Name " + selectedProductID, productPrice.toString(), productQuantity);
        OrdersTable.getItems().add(newItem);
        OrdersTable.refresh();

        // Call updateTotalPrice to refresh the total value
        updateTotalPrice();
    }

    @FXML
    private void QueueClicked(MouseEvent event) {
        MainTab.getSelectionModel().select(OrderQueueTab);
        InputTab.getSelectionModel().select(OrderDetails);
    }

    @FXML
    private void OrdersClicked(MouseEvent event) {
        MainTab.getSelectionModel().select(OrdersTab);
        InputTab.getSelectionModel().select(OrderInput);
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (OrderItem item : OrdersTable.getItems()) {
            // Get the numeric value of the meal price, removing any non-numeric characters (like ₱)
            String priceStr = item.getMealPrice().replaceAll("[^\\d.]", "");
            double itemPrice = Double.parseDouble(priceStr);

            // Calculate the total for this item (price * quantity)
            total += itemPrice * item.getQuantity();
        }

        // Update the TotalText to show the new total value
        TotalText.setText(String.format("₱%.2f", total));
    }



    @FXML
    private void PayClicked(MouseEvent event) {
        // Get the total amount from TotalText
        String totalTextValue = TotalText.getText().replaceAll("[^\\d.]", "");  // Remove the ₱ symbol and get the number
        double totalAmount = Double.parseDouble(totalTextValue);

        // Get the amount from the AmountTextField (money given by the customer)
        String amountGivenText = AmountTextField.getText();
        if (amountGivenText.isEmpty()) {
            showAlert("Error", "Please enter the amount given by the customer");
            return;
        }

        double amountGiven = Double.parseDouble(amountGivenText);

        // Calculate the change
        if (amountGiven < totalAmount) {
            showAlert("Error", "Amount given is less than the total. Please enter a valid amount.");
            return;
        }

        double changeAmount = amountGiven - totalAmount;

        // Update the ChangeText with the calculated change
        ChangeText.setText(String.format("₱%.2f", changeAmount));
    }


    @FXML
    private void RecieptClicked(MouseEvent event) {
        String orderNumber = OrderNumberInput.getText().trim();  // Get the order number
        if (orderNumber.isEmpty()) {
            showAlert("Error", "Please enter an order number");
            return;
        }

        Stack<OrderItem> orderStack = orderStacks.get(orderNumber);  // Get the items for this order
        if (orderStack == null || orderStack.isEmpty()) {
            showAlert("Error", "No items found for order number: " + orderNumber);
            return;
        }

        // Prepare the receipt content
        StringBuilder receiptContent = new StringBuilder();
        receiptContent.append("Order Receipt\n");
        receiptContent.append("Order Number: ").append(orderNumber).append("\n");
        receiptContent.append("======================================\n");

        // Loop through the order items and add to receipt
        double totalPrice = 0.0;
        for (OrderItem item : orderStack) {
            String mealName = item.getMealName();
            double price = Double.parseDouble(item.getMealPrice().replaceAll("[^\\d.]", ""));
            int quantity = item.getQuantity();
            double itemTotal = price * quantity;
            totalPrice += itemTotal;

            receiptContent.append(String.format("%-20s %5d x ₱%.2f = ₱%.2f\n", mealName, quantity, price, itemTotal));
        }

        receiptContent.append("======================================\n");
        receiptContent.append(String.format("Total: ₱%.2f\n", totalPrice));

        // Get the amount paid by the customer
        String amountPaidText = AmountTextField.getText();
        if (amountPaidText.isEmpty()) {
            showAlert("Error", "Please enter the amount paid by the customer.");
            return;
        }
        double amountPaid = Double.parseDouble(amountPaidText);

        // Get the change (this should already be calculated when the Pay button is clicked)
        String changeText = ChangeText.getText().replaceAll("[^\\d.]", "");  // Remove any currency symbols
        double changeAmount = Double.parseDouble(changeText);

        // Add the amount paid and change to the receipt
        receiptContent.append(String.format("Amount Paid: ₱%.2f\n", amountPaid));
        receiptContent.append(String.format("Change: ₱%.2f\n", changeAmount));

        // Write receipt to a file named after the order number
        String fileName = "Receipt_" + orderNumber + ".txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(receiptContent.toString());
            showAlert("Success", "Receipt saved as " + fileName);
        } catch (IOException e) {
            showAlert("Error", "Failed to save receipt: " + e.getMessage());
        }
    }



    @FXML
    private void RemoveClicked(MouseEvent event) {
        OrderItem selectedItem = OrdersTable.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            OrdersTable.getItems().remove(selectedItem);
            OrdersTable.refresh();
            // Call updateTotalPrice to refresh the total value
            updateTotalPrice();
        } else {
            showAlert("Error", "Please select an item to remove");
        }
    }



}
