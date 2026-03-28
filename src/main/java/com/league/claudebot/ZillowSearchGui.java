package com.league.claudebot;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.List;

public class ZillowSearchGui extends JFrame {

    private final ZillowApiClient apiClient;

    private JTextField keywordField;
    private JComboBox<String> typeCombo;
    private JButton searchButton;
    private JButton prevButton;
    private JButton nextButton;
    private JLabel pageLabel;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private static final String[] COLUMNS = {
        "Address", "City", "State", "Price", "Beds", "Baths", "Sq Ft", "Type", "Days Listed", "Status"
    };

    private List<Property> currentProperties;
    private int currentPage = 1;
    private int totalPages = 1;
    private String lastKeyword = "";
    private String lastType = "forSale";

    public ZillowSearchGui() {
        this.apiClient = new ZillowApiClient();
        initUI();
    }

    private void initUI() {
        setTitle("ClaudeBot - Zillow Property Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        add(buildSearchPanel(), BorderLayout.NORTH);
        add(buildResultsPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.setBackground(new Color(30, 50, 80));

        JLabel titleLabel = new JLabel("Zillow Search");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JLabel kwLabel = new JLabel("Keyword:");
        kwLabel.setForeground(Color.WHITE);

        keywordField = new JTextField(20);
        keywordField.setToolTipText("e.g. Miami FL, Seattle WA, 90210");

        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setForeground(Color.WHITE);

        typeCombo = new JComboBox<>(new String[]{"forSale", "forRent", "sold"});

        searchButton = new JButton("Search");
        searchButton.setBackground(new Color(0, 120, 200));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        prevButton = new JButton("< Prev");
        prevButton.setForeground(Color.WHITE);
        prevButton.setBackground(new Color(60, 90, 130));
        prevButton.setFocusPainted(false);
        prevButton.setEnabled(false);

        nextButton = new JButton("Next >");
        nextButton.setForeground(Color.WHITE);
        nextButton.setBackground(new Color(60, 90, 130));
        nextButton.setFocusPainted(false);
        nextButton.setEnabled(false);

        pageLabel = new JLabel("Page 1 of 1");
        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(100, 20));

        searchButton.addActionListener(e -> {
            currentPage = 1;
            performSearch();
        });
        keywordField.addActionListener(e -> {
            currentPage = 1;
            performSearch();
        });
        prevButton.addActionListener(e -> {
            currentPage--;
            fetchPage();
        });
        nextButton.addActionListener(e -> {
            currentPage++;
            fetchPage();
        });

        panel.add(titleLabel);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(kwLabel);
        panel.add(keywordField);
        panel.add(typeLabel);
        panel.add(typeCombo);
        panel.add(searchButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(prevButton);
        panel.add(pageLabel);
        panel.add(nextButton);
        panel.add(progressBar);

        return panel;
    }

    private JScrollPane buildResultsPanel() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultsTable.setRowHeight(24);
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setGridColor(new Color(220, 220, 220));

        // Alternate row colors with right-aligned numeric columns
        resultsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(245, 248, 255));
                }
                if (column == 3 || column == 4 || column == 5 || column == 6 || column == 8) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        });

        // Double-click opens listing in browser
        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && currentProperties != null) {
                    int row = resultsTable.getSelectedRow();
                    if (row >= 0 && row < currentProperties.size()) {
                        openUrl(currentProperties.get(row).getUrl());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    private JPanel buildStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));

        statusLabel = new JLabel("Enter a keyword to search for properties.");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel hint = new JLabel("  |  Double-click a row to open listing in browser.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Color.GRAY);

        panel.add(statusLabel);
        panel.add(hint);
        return panel;
    }

    private void performSearch() {
        String keyword = keywordField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search keyword.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        lastKeyword = keyword;
        lastType = (String) typeCombo.getSelectedItem();
        fetchPage();
    }

    private void fetchPage() {
        setControlsEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        statusLabel.setText("Searching...");
        tableModel.setRowCount(0);

        final int pageToFetch = currentPage;
        final String keyword = lastKeyword;
        final String type = lastType;

        SwingWorker<ZillowApiClient.SearchResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ZillowApiClient.SearchResult doInBackground() throws Exception {
                return apiClient.search(keyword, type, pageToFetch);
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                try {
                    ZillowApiClient.SearchResult result = get();
                    currentPage = result.getCurrentPage();
                    totalPages = result.getTotalPages();
                    currentProperties = result.getProperties();
                    populateTable(currentProperties);
                    pageLabel.setText("Page " + currentPage + " of " + totalPages);
                    prevButton.setEnabled(currentPage > 1);
                    nextButton.setEnabled(currentPage < totalPages);
                    statusLabel.setText(String.format(
                        "Page %d of %d  |  Showing %d properties  |  %,d total results for \"%s\"",
                        currentPage, totalPages, currentProperties.size(), result.getTotalResults(), keyword));
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(ZillowSearchGui.this,
                            "Search failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private void setControlsEnabled(boolean enabled) {
        searchButton.setEnabled(enabled);
        keywordField.setEnabled(enabled);
        typeCombo.setEnabled(enabled);
        prevButton.setEnabled(enabled && currentPage > 1);
        nextButton.setEnabled(enabled && currentPage < totalPages);
    }

    private void populateTable(List<Property> properties) {
        tableModel.setRowCount(0);
        for (Property p : properties) {
            tableModel.addRow(new Object[]{
                p.getStreet(),
                p.getCity(),
                p.getState(),
                p.getFormattedPrice(),
                p.getBeds() > 0 ? p.getBeds() : "—",
                p.getBaths() > 0 ? String.format("%.1f", p.getBaths()) : "—",
                p.getArea() > 0 ? String.format("%.0f", p.getArea()) : "—",
                formatHomeType(p.getHomeType()),
                p.getDaysOnZillow(),
                formatStatus(p.getStatus())
            });
        }
    }

    private String formatHomeType(String type) {
        if (type == null) return "";
        String lower = type.replace("_", " ").toLowerCase();
        return lower.substring(0, 1).toUpperCase() + lower.substring(1);
    }

    private String formatStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "FOR_SALE" -> "For Sale";
            case "FOR_RENT" -> "For Rent";
            case "SOLD" -> "Sold";
            default -> status;
        };
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not open browser:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
