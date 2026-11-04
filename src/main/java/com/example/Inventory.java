package com.example;

import java.util.HashMap;

public class Inventory {

	static private HashMap<Product, Integer> inventory;
	
	Inventory() {
		inventory = new HashMap<>();
	}
	
	/// Used to add a product to inventory.
	static public void addNewProduct(Product newProduct) {
		inventory.put(newProduct, 0);
	}
	
	/// Used to load products from save file.
	static public void addNewProduct(Product newProduct, int stock) {
		inventory.put(newProduct, stock);
	}
	
	/// Manager can restock existing products. Error Handling for product nonexistence.
	@SuppressWarnings("unused")
	public void restockProduct(Product itemToRestock, int newStock) {
		
		if (inventory.containsKey(itemToRestock)) {
			inventory.merge(itemToRestock, newStock, (a, b) -> b);
		} else {
			System.out.println("Product Does Not Exist");
			return;
		}
		
	} // End of restockProduct()
	
	/// Clerk handling customer purchases. Error Handling for valid product and amount bought.
	public void boughtProducts(Product boughtItem, int amount) {
		
		if (inventory.containsKey(boughtItem)) {
			if (amount <= inventory.get(boughtItem)) {
				
			} else {
				System.out.println("Not Enough Stock");
				return;
			}
		} else {
			System.out.println("Product Does Not Exist");
			return;
		}
		
	} // End of boughtProducts()
	
} // End of class Inventory
