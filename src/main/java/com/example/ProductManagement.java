package com.example;

import java.util.ArrayList;

public class ProductManagement {

	private ArrayList<Product> productList;

	ProductManagement () {
		productList = new ArrayList<>();
	}
	
	/// Load Products From Save File
	public void loadProducts() {
		
	}
	
	/// Manager can add a new product to business. Will automatically add to Inventory set as zero stock.
	public void addProduct(String name, String description, int price) {
		Product newProduct = new Product(name, description, price);
		productList.add(newProduct);
		Inventory.addNewProduct(newProduct);
	}
	
}
