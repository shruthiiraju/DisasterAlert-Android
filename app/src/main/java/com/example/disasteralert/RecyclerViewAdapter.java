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

import java.util.ArrayList;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{

    private static final String TAG = "RecyclerViewAdapter";

    private ArrayList<String> Type = new ArrayList<>();
    private ArrayList<String> Desc = new ArrayList<>();
    private ArrayList<String> mImages = new ArrayList<>();
    private ArrayList<String> colour;
    private Context mContext;

    public RecyclerViewAdapter(Context mContext, ArrayList<String> type, ArrayList<String> desc,
                               ArrayList<String> mImages, ArrayList<String> colour){
        this.mContext = mContext;
        this.Type = type;
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
            Glide.with(mContext)
                    .asBitmap()
                    .load(mImages.get(position))
                    .into(holder.image);
        }
        holder.type.setText(Type.get(position));
        holder.desc.setText(Desc.get(position));
        if(colour.get(position).equals("red")){
            holder.parent_layout.setBackgroundColor(Color.parseColor("#E84342"));
        }
        else if (colour.get(position).equals("green")){
            holder.parent_layout.setBackgroundColor(Color.parseColor("#badc57"));
        }
        else
            holder.parent_layout.setBackgroundColor(Color.parseColor("#EA7773"));

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

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView type, desc;
        ImageView image;
        RelativeLayout parent_layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.itemImage);
            type = itemView.findViewById(R.id.itemText);
            desc = itemView.findViewById(R.id.itemDesc);
            parent_layout = itemView.findViewById(R.id.cardLayout);
        }
    }
}