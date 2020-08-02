package com.example.disasteralert;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.ocpsoft.prettytime.PrettyTime;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> Type = new ArrayList<>();
    private ArrayList<String> Timestamp = new ArrayList<>();
    private ArrayList<String> Desc = new ArrayList<>();
    private ArrayList<String> mImages = new ArrayList<>();
    private ArrayList<String> colour;
    private Context mContext;

    PrettyTime p = new PrettyTime();

    public RecyclerViewAdapter(Context mContext, ArrayList<String> type, ArrayList<String> timestamp, ArrayList<String> desc,
                               ArrayList<String> mImages, ArrayList<String> colour){
        this.mContext = mContext;
        this.Type = type;
        this.Timestamp = timestamp;
        this.Desc = desc;
        this.colour = colour;
        this.mImages = mImages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder viewholder = new ViewHolder(view);
        return viewholder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called. " + mImages.get(position));
        if(mImages.get(position) == null){
            holder.image.setVisibility(View.GONE);
        }
        else {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .asBitmap()
                    .load(mImages.get(position))
                    .into(holder.image);
        }
        holder.type.setText(Type.get(position));
        holder.desc.setText(Desc.get(position));
        String dateString = p.format(new Date(Long.parseLong(Timestamp.get(position))));
        holder.timestamp.setText(dateString);
        if(colour.get(position).equals("red")){
            holder.parent_layout.setBackgroundColor(Color.parseColor("#D84315"));
        }
        else if (colour.get(position).equals("green")){
            holder.parent_layout.setBackgroundColor(Color.parseColor("#43A047"));
        }
        else
            holder.parent_layout.setBackgroundColor(Color.parseColor("#FFA000"));

        holder.parent_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked on" + Type.get(position));
                Toast.makeText(mContext, Type.get(position), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return Type.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        TextView type, desc, timestamp;
        ImageView image;
        RelativeLayout parent_layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemImage);
            type = itemView.findViewById(R.id.itemText);
            desc = itemView.findViewById(R.id.itemDesc);
            timestamp = itemView.findViewById(R.id.itemTimestamp);
            parent_layout = itemView.findViewById(R.id.cardLayout);
        }
    }
}