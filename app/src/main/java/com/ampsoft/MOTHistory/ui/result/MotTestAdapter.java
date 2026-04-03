package com.ampsoft.MOTHistory.ui.result;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ampsoft.MOTHistory.R;
import com.ampsoft.MOTHistory.data.model.Defect;
import com.ampsoft.MOTHistory.data.model.MotTest;
import com.ampsoft.MOTHistory.util.DateFormatter;

import java.util.ArrayList;
import java.util.List;

public class MotTestAdapter extends RecyclerView.Adapter<MotTestAdapter.MotTestViewHolder> {

    private final List<MotTest> motTests = new ArrayList<>();
    private final List<Boolean> expandedStates = new ArrayList<>();

    public void submitList(List<MotTest> tests) {
        motTests.clear();
        expandedStates.clear();
        if (tests != null) {
            motTests.addAll(tests);
            for (int i = 0; i < tests.size(); i++) {
                expandedStates.add(false);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MotTestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mot_test, parent, false);
        return new MotTestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MotTestViewHolder holder, int position) {
        holder.bind(motTests.get(position), expandedStates.get(position));
        holder.itemView.setOnClickListener(v -> toggleExpanded(position));
    }

    @Override
    public int getItemCount() {
        return motTests.size();
    }

    private void toggleExpanded(int position) {
        if (position < 0 || position >= expandedStates.size()) {
            return;
        }
        expandedStates.set(position, !expandedStates.get(position));
        notifyItemChanged(position);
    }

    static class MotTestViewHolder extends RecyclerView.ViewHolder {

        private final View header;
        private final TextView date;
        private final TextView result;
        private final TextView expiry;
        private final TextView mileage;
        private final LinearLayout summaryCountsLayout;
        private final TextView advisories;
        private final TextView failures;
        private final TextView toggleDetails;
        private final LinearLayout detailsLayout;
        private final LinearLayout defectListLayout;
        private final TextView noDetails;

        MotTestViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.layout_mot_header);
            date = itemView.findViewById(R.id.tv_mot_date);
            result = itemView.findViewById(R.id.tv_mot_result);
            expiry = itemView.findViewById(R.id.tv_mot_expiry);
            mileage = itemView.findViewById(R.id.tv_mot_mileage);
            summaryCountsLayout = itemView.findViewById(R.id.layout_mot_summary_counts);
            advisories = itemView.findViewById(R.id.tv_mot_advisories);
            failures = itemView.findViewById(R.id.tv_mot_failures);
            toggleDetails = itemView.findViewById(R.id.tv_mot_toggle_details);
            detailsLayout = itemView.findViewById(R.id.layout_mot_details);
            defectListLayout = itemView.findViewById(R.id.layout_mot_defect_list);
            noDetails = itemView.findViewById(R.id.tv_mot_no_details);
        }

        void bind(MotTest motTest, boolean expanded) {
            StatusStyle style = resolveStatusStyle(motTest);
            int advisoryCount = countAdvisories(motTest.getDefects());
            int failureCount = countFailures(motTest.getDefects());
            boolean hasDetails = motTest.getDefects() != null && !motTest.getDefects().isEmpty();
            boolean isCleanPass = failureCount == 0 && advisoryCount == 0 && style.cleanPass;

            date.setText(DateFormatter.asDisplayDate(motTest.getCompletedDate()));
            result.setText(style.label);
            header.setBackgroundColor(style.backgroundColor);
            expiry.setText(itemView.getContext().getString(
                    R.string.mot_test_expiry_value,
                    DateFormatter.asDisplayDate(motTest.getExpiryDate())
            ));
            mileage.setText(itemView.getContext().getString(
                    R.string.mot_test_mileage_value,
                    buildMileage(motTest.getOdometerValue(), motTest.getOdometerUnit())
            ));
            summaryCountsLayout.setVisibility(isCleanPass ? View.GONE : View.VISIBLE);

            advisories.setText(itemView.getContext().getString(
                    R.string.mot_test_advisories_value,
                    String.valueOf(advisoryCount)
            ));
            advisories.setVisibility(advisoryCount > 0 ? View.VISIBLE : View.GONE);

            failures.setText(itemView.getContext().getString(
                    R.string.mot_test_failures_value,
                    String.valueOf(failureCount)
            ));
            failures.setVisibility(failureCount > 0 ? View.VISIBLE : View.GONE);

            toggleDetails.setVisibility(hasDetails && !isCleanPass ? View.VISIBLE : View.GONE);
            toggleDetails.setText(expanded
                    ? R.string.mot_test_details_hide
                    : R.string.mot_test_details_show);
            detailsLayout.setVisibility(hasDetails && expanded ? View.VISIBLE : View.GONE);
            bindDefectDetails(motTest.getDefects());
        }

        private String buildMileage(String value, String unit) {
            String safeValue = fallback(value);
            String safeUnit = fallback(unit);
            if ("-".equals(safeValue) && "-".equals(safeUnit)) {
                return "-";
            }
            return (safeValue + " " + safeUnit).trim();
        }

        private String fallback(String value) {
            return value == null || value.trim().isEmpty() ? "-" : value;
        }

        private int countAdvisories(List<Defect> defectList) {
            int count = 0;
            if (defectList == null) {
                return 0;
            }
            for (Defect defect : defectList) {
                if (defect != null && "ADVISORY".equalsIgnoreCase(normalizeType(defect.getType()))) {
                    count++;
                }
            }
            return count;
        }

        private int countFailures(List<Defect> defectList) {
            int count = 0;
            if (defectList == null) {
                return 0;
            }
            for (Defect defect : defectList) {
                if (defect == null) {
                    continue;
                }
                String type = normalizeType(defect.getType());
                if ("FAIL".equals(type) || "MAJOR".equals(type) || "DANGEROUS".equals(type)) {
                    count++;
                }
            }
            return count;
        }

        private void bindDefectDetails(List<Defect> defectList) {
            defectListLayout.removeAllViews();
            boolean hasDetails = defectList != null && !defectList.isEmpty();
            noDetails.setVisibility(hasDetails ? View.GONE : View.VISIBLE);
            if (!hasDetails) {
                return;
            }

            for (Defect defect : defectList) {
                if (defect == null) {
                    continue;
                }

                TextView detailView = new TextView(itemView.getContext());
                detailView.setTextAppearance(com.google.android.material.R.style.TextAppearance_MaterialComponents_Body2);
                detailView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                detailView.setPadding(0, 0, 0, (int) dp(8));
                detailView.setTypeface(Typeface.DEFAULT_BOLD);

                String type = fallback(normalizeType(defect.getType()));
                String text = fallback(defect.getText());
                detailView.setText(type + ": " + text);
                defectListLayout.addView(detailView);
            }
        }

        private StatusStyle resolveStatusStyle(MotTest motTest) {
            String normalizedResult = normalizeType(motTest.getTestResult());
            if (isFailureResult(normalizedResult)) {
                return new StatusStyle(
                        itemView.getContext().getString(R.string.mot_test_status_fail),
                        ContextCompat.getColor(itemView.getContext(), R.color.status_fail),
                        false
                );
            }
            if (countAdvisories(motTest.getDefects()) > 0) {
                return new StatusStyle(
                        itemView.getContext().getString(R.string.mot_test_status_pass_advisories),
                        ContextCompat.getColor(itemView.getContext(), R.color.status_warning),
                        false
                );
            }
            if (isPassResult(normalizedResult)) {
                return new StatusStyle(
                        itemView.getContext().getString(R.string.mot_test_status_pass),
                        ContextCompat.getColor(itemView.getContext(), R.color.status_pass),
                        true
                );
            }
            return new StatusStyle(
                    fallback(normalizedResult),
                    ContextCompat.getColor(itemView.getContext(), R.color.status_neutral),
                    false
            );
        }

        private String normalizeType(String value) {
            return value == null ? "" : value.trim().toUpperCase();
        }

        private boolean isFailureResult(String normalizedResult) {
            return normalizedResult.contains("FAIL");
        }

        private boolean isPassResult(String normalizedResult) {
            return normalizedResult.contains("PASS");
        }

        private float dp(float value) {
            return value * itemView.getResources().getDisplayMetrics().density;
        }
    }

    static class StatusStyle {
        final String label;
        final int backgroundColor;
        final boolean cleanPass;

        StatusStyle(String label, int backgroundColor, boolean cleanPass) {
            this.label = label;
            this.backgroundColor = backgroundColor;
            this.cleanPass = cleanPass;
        }
    }
}
