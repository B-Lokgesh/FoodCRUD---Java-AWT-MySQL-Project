import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*; 

public class FoodCRUD extends Frame implements ActionListener, ItemListener {
    TextField idField, categoryField;
    Choice nameChoice;
    Choice priceChoice;
    List itemList;
    Button addBtn, updateBtn, deleteBtn, loadBtn, clearBtn;
    Connection con;

    String[][] foods = {
        {"Idly", "Veg"}, {"Dosa", "Veg"}, {"Pongal", "Veg"}, {"Poori", "Veg"},
        {"Veg Fried Rice", "Veg"}, {"Paneer Butter Masala", "Veg"}, {"Veg Biryani", "Veg"},
        {"Chapathi", "Veg"}, {"Curd Rice", "Veg"}, {"Upma", "Veg"},
        {"Chicken Biryani", "Non-Veg"}, {"Mutton Biryani", "Non-Veg"},
        {"Fish Fry", "Non-Veg"}, {"Chicken 65", "Non-Veg"}, {"Egg Curry", "Non-Veg"},
        {"Grill Chicken", "Non-Veg"}, {"Butter Chicken", "Non-Veg"},
        {"Prawn Curry", "Non-Veg"}, {"Egg Fried Rice", "Non-Veg"}, {"Chicken Noodles", "Non-Veg"}
    };

    public FoodCRUD() {
        super("Food Management System");
        setLayout(new BorderLayout(5,5));
        setSize(700, 450);

       
        Panel form = new Panel(new GridLayout(5,2,5,5));

        form.add(new Label("ID:"));
        idField = new TextField(); idField.setEditable(false); form.add(idField);

        form.add(new Label("Food Name:"));
        nameChoice = new Choice();
        for (String[] f : foods) nameChoice.add(f[0]);
        form.add(nameChoice);

        form.add(new Label("Category:"));
        categoryField = new TextField(); categoryField.setEditable(false); form.add(categoryField);

        form.add(new Label("Price:"));
        priceChoice = new Choice();
        for (int p = 40; p <= 500; p += 10) priceChoice.add("₹" + p);
        form.add(priceChoice);

        
        Panel buttons = new Panel(new GridLayout(1,5,5,5));
        addBtn = new Button("Add"); updateBtn = new Button("Update");
        deleteBtn = new Button("Delete"); loadBtn = new Button("Load"); clearBtn = new Button("Clear");
        for (Button b : new Button[]{addBtn, updateBtn, deleteBtn, loadBtn, clearBtn}) b.addActionListener(this);
        buttons.add(addBtn); buttons.add(updateBtn); buttons.add(deleteBtn); buttons.add(loadBtn); buttons.add(clearBtn);

       
        itemList = new List();
        itemList.addItemListener(this);

        add(form, BorderLayout.NORTH);
        add(itemList, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        connectDB();
        loadItems();

       
        nameChoice.addItemListener(e -> updateCategory());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        setVisible(true);
        updateCategory();
    }

    void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/fooddb?serverTimezone=UTC", "root", "root");
        } catch (Exception e) {
            System.out.println("DB Error: " + e);
        }
    }

    void updateCategory() {
        String selectedFood = nameChoice.getSelectedItem();
        for (String[] f : foods) {
            if (f[0].equals(selectedFood)) {
                categoryField.setText(f[1]);
                break;
            }
        }
    }

    void loadItems() {
        itemList.removeAll();
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM food_items")) {
            while (rs.next())
                itemList.add(rs.getInt("id")+" | "+rs.getString("name")+" | "+rs.getString("category")+" | ₹"+rs.getDouble("price"));
        } catch (Exception e) { System.out.println("Load Error: " + e); }
    }

    void addItem() {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO food_items (name,category,price) VALUES(?,?,?)")) {
            ps.setString(1, nameChoice.getSelectedItem());
            ps.setString(2, categoryField.getText());
            ps.setDouble(3, Double.parseDouble(priceChoice.getSelectedItem().replace("₹", "")));
            ps.executeUpdate();
            loadItems();

          
            JOptionPane.showMessageDialog(null, "✅ New item added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) { 
            System.out.println("Add Error: " + e); 
            JOptionPane.showMessageDialog(null, "❌ Error adding item:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    void updateItem() {
        if (idField.getText().isEmpty()) return;
        try (PreparedStatement ps = con.prepareStatement("UPDATE food_items SET name=?,category=?,price=? WHERE id=?")) {
            ps.setString(1, nameChoice.getSelectedItem());
            ps.setString(2, categoryField.getText());
            ps.setDouble(3, Double.parseDouble(priceChoice.getSelectedItem().replace("₹", "")));
            ps.setInt(4, Integer.parseInt(idField.getText()));
            ps.executeUpdate();
            loadItems();
            JOptionPane.showMessageDialog(null, "✅ Item updated successfully!", "Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) { 
            System.out.println("Update Error: " + e); 
        }
    }

    void deleteItem() {
        if (idField.getText().isEmpty()) return;
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM food_items WHERE id=?")) {
            ps.setInt(1, Integer.parseInt(idField.getText()));
            ps.executeUpdate();
            loadItems();
            JOptionPane.showMessageDialog(null, "🗑️ Item deleted successfully!", "Deleted", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) { 
            System.out.println("Delete Error: " + e); 
        }
    }

    void clearFields() {
        idField.setText("");
        nameChoice.select(0);
        updateCategory();
        priceChoice.select(0);
    }

    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == addBtn) addItem();
        else if (src == updateBtn) updateItem();
        else if (src == deleteBtn) deleteItem();
        else if (src == loadBtn) loadItems();
        else if (src == clearBtn) clearFields();
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            String s = itemList.getSelectedItem();
            int id = Integer.parseInt(s.split("\\|")[0].trim());
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM food_items WHERE id=?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    idField.setText(rs.getString("id"));
                    nameChoice.select(rs.getString("name"));
                    updateCategory();
                    priceChoice.select("₹" + (int) rs.getDouble("price"));
                }
            } catch (Exception ex) { System.out.println("Select Error: " + ex); }
        }
    }

    public static void main(String[] args) {
        new FoodCRUD();
    }
}
