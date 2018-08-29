package com.google.firebase.quickstart.firebasestorage;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private List<Data> mDataset;

    private RecyclerViewClickListener mListener;
    public ButtonListener onClickListener;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public ImageView imageView;
        public TextView mTextView;
        public View button ;
        private View delete;
        private RecyclerViewClickListener mListener;

        public ViewHolder(View v  , RecyclerViewClickListener listener ) {
            super(v);
            mTextView = (TextView)v.findViewById(R.id.tv_android);
            imageView = (ImageView)v.findViewById(R.id.img_android);
            button = (View)v.findViewById(R.id.upload);
            delete = v.findViewById(R.id.Delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.deleteOnClick(v, MyAdapter.this.mDataset.get(getAdapterPosition()));
                }
            });
            button.setOnClickListener(this);
            mListener = listener;
        }

        @Override
        public void onClick(View view) {

            mListener.onClick(view, MyAdapter.this.mDataset.get(getAdapterPosition()));

        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(RecyclerViewClickListener listener , ButtonListener buttonListener) {
        this.mListener = listener;
        this.onClickListener = buttonListener;
    }


    public void setDatas(List<Data> datas) {
        mDataset = datas;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_text_view, parent, false);

        return new ViewHolder(v , mListener);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Data selectedData = mDataset.get(position);
        holder.mTextView.setText("stage" + selectedData.stage);
        Uri uri = Uri.parse(selectedData.path);
        Context context = holder.imageView.getContext();
        Picasso.with(context).load(uri).fit().into(holder.imageView);
       // holder.imageView.setImageURI(uri);
        if (selectedData.isUpload) {
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(view.getContext(), "Already uploaded", Toast.LENGTH_LONG)
                            .show();
                }
            });
            holder.mTextView.setText("stage" + selectedData.stage + "uploaded");

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(view.getContext(), "Already uploaded", Toast.LENGTH_LONG)
                            .show();
                }
            });

        }
    }



    public interface ButtonListener {

        void deleteOnClick(View v, Data position);

    }
    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mDataset != null) {
            return mDataset.size();
        }

        return 0;
    }
}
