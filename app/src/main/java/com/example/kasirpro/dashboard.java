package com.example.kasirpro;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;

public class dashboard extends AppCompatActivity {

    private TextView tvRevenue;
    private ImageView btnBack;
    private EditText etMM, etYYYY;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        tvRevenue = findViewById(R.id.tvRevenueDashboard);
        btnBack = findViewById(R.id.btnBack);
        etMM = findViewById(R.id.et_mm);
        etYYYY = findViewById(R.id.et_yyyy);
        lineChart = findViewById(R.id.lineChart);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);

        etMM.setText(String.format("%02d", currentMonth));
        etYYYY.setText(String.valueOf(currentYear));

        TextWatcher filterWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateDashboardLogic();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etMM.addTextChangedListener(filterWatcher);
        etYYYY.addTextChangedListener(filterWatcher);

        updateDashboardLogic();
    }

    private void updateDashboardLogic() {
        String mm = etMM.getText().toString().trim();
        String yyyy = etYYYY.getText().toString().trim();

        displayRevenueByFilter(mm, yyyy);

        // Perubahan: Grafik akan selalu mencoba setup selama YYYY tidak kosong
        if (yyyy.isEmpty()) {
            lineChart.setVisibility(View.GONE);
        } else {
            setupLineChart(yyyy);
        }
    }

    private void displayRevenueByFilter(String mm, String yyyy) {
        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        String currentUser = session.getString("logged_in_user", "default");

        SharedPreferences sharedPref = getSharedPreferences("DATA_REVENUE_" + currentUser, MODE_PRIVATE);

        String key = "REV_" + mm + "_" + yyyy;
        long revenue = sharedPref.getLong(key, 0);

        String formattedHarga = String.format("%,d", revenue).replace(',', '.');
        tvRevenue.setText("Rp : " + formattedHarga);
    }

    private void setupLineChart(String yyyy) {
        SharedPreferences session = getSharedPreferences("SESSION_KASIR", MODE_PRIVATE);
        String currentUser = session.getString("logged_in_user", "default");
        SharedPreferences sharedPref = getSharedPreferences("DATA_REVENUE_" + currentUser, MODE_PRIVATE);

        ArrayList<Entry> entries = new ArrayList<>();

        // Loop 12 bulan
        for (int i = 1; i <= 12; i++) {
            String monthKey = String.format("%02d", i);
            // Default 0: Jika akun baru, semua 'val' akan menjadi 0
            long val = sharedPref.getLong("REV_" + monthKey + "_" + yyyy, 0);
            entries.add(new Entry(i - 1, (float) val));
        }

        // --- FIX: Grafik tetap VISIBLE meskipun datanya 0 ---
        lineChart.setVisibility(View.VISIBLE);

        final String[] months = new String[]{"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
        LineDataSet dataSet = new LineDataSet(entries, "Penjualan " + yyyy);

        // Styling
        dataSet.setColor(Color.parseColor("#5C6BC0"));
        dataSet.setCircleColor(Color.parseColor("#5C6BC0"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f); // Tetap muncul titik meski 0
        dataSet.setDrawCircleHole(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR); // Gunakan LINEAR agar garis lurus terlihat jelas
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#E8EAF6"));

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Atur Sumbu X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12);

        // Atur Sumbu Y agar selalu mulai dari 0 meskipun data kosong
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        // Jika semua data 0, kita beri range maksimum 100.000 agar grafik tidak "patah"
        leftAxis.setAxisMaximum(Math.max(100000f, lineData.getYMax() + 50000f));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.animateY(800);
        lineChart.invalidate();
    }
}