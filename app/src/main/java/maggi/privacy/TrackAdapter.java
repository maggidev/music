package maggi.privacy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

public class TrackAdapter extends ListAdapter<Track, TrackAdapter.ViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Track track);
    }

    public TrackAdapter(OnItemClickListener listener) {
        super(new TrackDiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_metro_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Track track = getItem(position);
        holder.tvTitle.setText(track.getTitle());
        holder.tvArtist.setText(track.getArtist());
        
        Glide.with(holder.itemView.getContext())
             .load(track.getThumbnailUrl())
             .placeholder(android.R.drawable.ic_media_play)
             .centerCrop()
             .into(holder.imgThumb);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(track));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist;
        ShapeableImageView imgThumb;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.txt_title);
            tvArtist = itemView.findViewById(R.id.txt_artist);
            imgThumb = itemView.findViewById(R.id.img_thumb);
        }
    }

    private static class TrackDiffCallback extends DiffUtil.ItemCallback<Track> {
        @Override
        public boolean areItemsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
            return oldItem.getVideoId().equals(newItem.getVideoId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) && 
                   oldItem.getArtist().equals(newItem.getArtist()) &&
                   oldItem.getThumbnailUrl().equals(newItem.getThumbnailUrl());
        }
    }
}
