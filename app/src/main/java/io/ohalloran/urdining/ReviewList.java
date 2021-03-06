package io.ohalloran.urdining;

import android.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.List;

import io.ohalloran.urdining.data.DataUtils;
import io.ohalloran.urdining.data.DiningHall;
import io.ohalloran.urdining.data.Review;

/**
 * Created by Ben on 1/29/2015.
 */
public class ReviewList extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String WHICH_KEY = "hall";
    private static final String MODE_KEY = "hall_mode";

    private DiningHall which;
    private Mode mode = Mode.RECENT;
    private SwipeRefreshLayout refreshLayout;

    public static ReviewList newInstance(DiningHall which) {
        ReviewList rl = new ReviewList();
        Bundle bun = new Bundle();
        bun.putString(WHICH_KEY, which.toString());
        rl.setArguments(bun);
        return rl;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        which = DiningHall.getEnum(getArguments().getString(WHICH_KEY));
        Mode m = Mode.valueOf(getArguments().getString(MODE_KEY, Mode.RECENT.toString()));
        updateMode(m);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_review, null);
        refreshLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setColorScheme(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(MODE_KEY, mode.toString());
        outState.putString(WHICH_KEY, which.toString());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ReviewAdapter ra = new ReviewAdapter();
        setListAdapter(ra);
        DataUtils.addBaseAdapter(ra);
        if (savedInstanceState != null) {
            which = DiningHall.valueOf(savedInstanceState.getString(WHICH_KEY));
            updateMode(Mode.valueOf(savedInstanceState.getString(MODE_KEY, Mode.RECENT.toString())));
            if(getListAdapter() != null) {
                getListAdapter().notifyDataSetChanged();
            }
        }
    }

    public DiningHall which() {
        return which;
    }

    @Override
    public ReviewAdapter getListAdapter() {
        return (ReviewAdapter) super.getListAdapter();
    }

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
        DataUtils.refreshReviews(new DataUtils.OnRefreshCallback() {
            @Override
            public void onRefreshComplete() {
                getListAdapter().notifyDataSetChanged();
                refreshLayout.setRefreshing(false);
            }
        });
    }

    public void updateMode(Mode mode) {
        if(this.mode != mode){
            this.mode = mode;
            if (getListAdapter() != null)
                getListAdapter().notifyDataSetChanged();
        }
    }

    public static enum Mode {
        RECENT, POPULAR
    }

    private class ReviewAdapter extends BaseAdapter {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View.OnTouchListener trueListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        };

        List<Review> reviews = DataUtils.getReviews(which);

        void fetchReviews() {
            switch (mode) {
                case RECENT:
                    reviews = DataUtils.getReviews(which);
                    break;
                case POPULAR:
                    reviews = DataUtils.getReviewsHot(which);
                    break;
            }
        }

        @Override
        public int getCount() {
            return reviews.size();
        }

        @Override
        public Review getItem(int position) {
            return reviews.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View root = convertView;
            if (convertView == null) {
                root = inflater.inflate(R.layout.review_layout, parent, false);
            }
            TextView textReview = (TextView) root.findViewById(R.id.text_review);
            RatingBar ratingBar = (RatingBar) root.findViewById(R.id.rating_display);
            final TextView scoreDisplay = (TextView) root.findViewById(R.id.vote_display);

            final Review data = getItem(position);

            final ImageView upArrow = (ImageView) root.findViewById(R.id.vote_up);
            final ImageView downArrow = (ImageView) root.findViewById(R.id.vote_down);

            //update the up/down arrow
            Integer oldVote = DataUtils.getVote(data);
            if (oldVote != null)
                if (oldVote == DataUtils.UP)
                    upArrow.setAlpha(1f);
                else if (oldVote == DataUtils.DOWN)
                    downArrow.setAlpha(1f);

            textReview.setText(data.getTextReview());
            ratingBar.setRating(data.getStartsReview());
            scoreDisplay.setText(data.getVotes() + "");

            ratingBar.setOnTouchListener(trueListener);

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    float upAlpha = .5f, downAlpha = .5f;
                    Integer i;
                    int flip;
                    if (v.getId() == R.id.vote_up) {
                        i = DataUtils.upVote(data);
                        flip = 1;
                    } else {
                        i = DataUtils.downVote(data);
                        flip = -1;
                    }

                    if (i == null) {
                        //no old vote, switch other
                        if (flip == 1)
                            upAlpha = 1f;
                        else
                            downAlpha = 1f;
                    } else if (i == flip) {
                        //both to neutral
                        upAlpha = .5f;
                        downAlpha = .5f;
                    } else {
                        //votes switched
                        if (flip == 1) {
                            downAlpha = .5f;
                            upAlpha = 1f;
                        } else {
                            upAlpha = .5f;
                            downAlpha = 1f;
                        }
                    }

                    upArrow.setAlpha(upAlpha);
                    downArrow.setAlpha(downAlpha);
                    scoreDisplay.setText(data.getVotes() + "");
                }
            };

            //up-down votes
            upArrow.setOnClickListener(listener);
            downArrow.setOnClickListener(listener);
            return root;
        }

        @Override
        public void notifyDataSetChanged() {
            fetchReviews();
            super.notifyDataSetChanged();
        }

    }
}
