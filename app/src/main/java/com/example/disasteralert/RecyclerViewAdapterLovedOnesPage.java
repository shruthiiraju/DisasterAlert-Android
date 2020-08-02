package com.example.disasteralert;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecyclerViewAdapterLovedOnesPage extends RecyclerView.Adapter<RecyclerViewAdapterLovedOnesPage.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapterLovedOnesPage";

    private ArrayList<String> numbers, names, images, safes;
    private Context mContext;

    RecyclerViewAdapterLovedOnesPage(Context mContext, ArrayList<String> numbers, ArrayList<String> names, ArrayList<String> images,
                                     ArrayList<String> safes){
        this.mContext = mContext;
        this.numbers = numbers;
        this.names = names;
        this.images = images;
        this.safes = safes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem_contacts, parent, false);
        ViewHolder viewholder = new ViewHolder(view);
        return viewholder;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: ");
        holder.num.setText(numbers.get(position));
        holder.head.setText(names.get(position));
        if(images.get(position) != null){
            Glide.with(mContext)
                    .asBitmap()
                    .load(images.get(position))
                    .into(holder.image);
        }
        if(safes.get(position) == null || safes.get(position).equals("true")){
            holder.parent_layout.setBackgroundColor(Color.parseColor("#43A047"));
        }
        else
            holder.parent_layout.setBackgroundColor(Color.parseColor("#D84315"));
    }

    @Override
    public int getItemCount() {
        return numbers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView head, num;
        CircleImageView image;
        RelativeLayout parent_layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.contactImage);
            head = itemView.findViewById(R.id.contactText);
            num = itemView.findViewById(R.id.contactNumber);
            parent_layout = itemView.findViewById(R.id.contactCardLayout);
        }
    }

}
