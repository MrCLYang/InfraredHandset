package com.zt.infraredhandset.RecycleView;

/*import android.bluetooth.BluetoothDevice;*/
import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zt.infraredhandset.R;

import java.util.List;

/**
 * Time:2019/11/1
 * Author:YCL
 * Description:
 */
public  class BluetoothAdapter extends RecyclerView.Adapter<BluetoothAdapter.ViewHolder> implements View.OnClickListener {
    private List<BluetoothDevice> mBtDevices;

    private OnItemClickListener itemClickListener;

    public BluetoothAdapter(List<BluetoothDevice> BtDevices) {
        mBtDevices = BtDevices;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booth, null);
        view.setOnClickListener(this);
        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BluetoothDevice device = mBtDevices.get(position);
        holder.bluetooth_address.setText(device.getAddress());//设置蓝牙地址
        String BtName = device.getName();
        if (BtName == null) {
            holder.bluetooth_name.setText("NULL");
        } else {
            holder.bluetooth_name.setText(BtName);
        }
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mBtDevices.size();
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onItemClick(v, (int) v.getTag());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView bluetooth_name;
        private TextView bluetooth_address;

        public ViewHolder(View itemView) {
            super(itemView);
            bluetooth_name = itemView.findViewById(R.id.item_booth_name);
            bluetooth_address = itemView.findViewById(R.id.item_booth_address);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener){
            this.itemClickListener=listener;
    }
}
