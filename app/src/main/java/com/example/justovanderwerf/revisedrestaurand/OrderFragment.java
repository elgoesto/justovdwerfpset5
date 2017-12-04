package com.example.justovanderwerf.revisedrestaurand;


import android.app.DialogFragment;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class OrderFragment extends DialogFragment implements View.OnClickListener {
    List<Dish> menu = new ArrayList<>();
    Items myGlob;
    RequestQueue queue;
    ListView orderListView;
    RestoDatabase db;
    RestoAdapter adapter;
    TextView totPrice;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        orderListView = view.findViewById(R.id.orderListView);

        db = RestoDatabase.getInstance(getActivity().getApplicationContext());

        adapter = new RestoAdapter(getActivity().getApplicationContext(), db.selectAll());

        orderListView.setAdapter(adapter);

        myGlob = new Items(getContext());

        queue = Volley.newRequestQueue(getActivity());

        String newUrl = "https://resto.mprog.nl/menu";

        totPrice = view.findViewById(R.id.totalPriceView);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, newUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        menu = myGlob.createMenu(response);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Error", "onErrorResponse: Error");
            }
        });
        queue.add(stringRequest);

        Button c = view.findViewById(R.id.cancelButton);
        c.setOnClickListener(this);

        Button o = view.findViewById(R.id.orderButton);
        o.setOnClickListener(this);

        totalPrice();

        return view;
    }



    private void order() {
        String newUrl = "https://resto.mprog.nl/order";

        StringRequest postRequest = new StringRequest(Request.Method.POST, newUrl,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        Integer duration = 0;

                        try {
                            JSONObject obj = new JSONObject(response);
                            duration = obj.getInt("preparation_time");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        String text = "Your order will take " + Integer.toString(duration) + " minutes.";

                        Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", "error");
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap();
                params.put("order", getOrderString());

                return params;
            }
        };
        queue.add(postRequest);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelButton:
                db.clear();
                adapter = new RestoAdapter(getActivity().getApplicationContext(), db.selectAll());
                orderListView.setAdapter(adapter);
                totPrice.setText("0");
                break;
            case R.id.orderButton:
                order();
                db.clear();
                adapter = new RestoAdapter(getActivity().getApplicationContext(), db.selectAll());
                orderListView.setAdapter(adapter);
                totPrice.setText("0");
                break;
        }
    }

    private String getOrderString() {
        SQLiteDatabase sqldb = db.getReadableDatabase();
        Cursor cursor = sqldb.rawQuery("SELECT name FROM 'order'", new String[] {});

        String orderString = "[";

        try {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex("name");
                String name = cursor.getString(nameIndex);
                Integer id = getDishByName(name).getId();
                orderString += id.toString() + ",";
            }
        } finally {
            cursor.close();
        }

        orderString += "]";

        return orderString;
    }

    private Dish getDishByName(String name) {
        Dish dish = new Dish();

        for(int i = 0; i < menu.size(); i++) {
            Dish temp = menu.get(i);
            if(temp.getName().equals(name)) {
                dish = temp;
            }
        }

        return dish;
    }
    private void totalPrice(){
        db = RestoDatabase.getInstance(getContext());
        Cursor cursor = db.selectAll();
        int total = 0;
        while (cursor.moveToNext()) {
            int priceIndex = cursor.getColumnIndex("price");
            int price = cursor.getInt(priceIndex);
            int amountIndex = cursor.getColumnIndex("amount");
            int amount = cursor.getInt(amountIndex);
            total += price * amount;


        }
        totPrice.setText(String.valueOf(total));

    }
}