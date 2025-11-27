package com.example.azimbalife.Utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueueManager {
    private DatabaseReference queueRef = FirebaseDatabase.getInstance()
            .getReference("MbararaHospital/Queues");

    public void addTokenToQueue(String tokenId, String doctorName, String specialty) {
        DatabaseReference doctorQueueRef = queueRef.child(doctorName).child("activeQueue");

        // Add token to the end of the queue
        doctorQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> queue = new ArrayList<>();

                // Get existing queue
                if (snapshot.exists()) {
                    for (DataSnapshot tokenSnap : snapshot.getChildren()) {
                        String existingToken = tokenSnap.getValue(String.class);
                        if (existingToken != null && !existingToken.equals(tokenId)) {
                            queue.add(existingToken);
                        }
                    }
                }

                // Add new token to the end
                queue.add(tokenId);

                // Update queue in Firebase
                updateQueueInFirebase(doctorName, queue);

                // Update all token positions
                updateAllTokenPositions(doctorName, queue, specialty);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QUEUE_MANAGER", "Error adding token to queue: " + error.getMessage());
            }
        });
    }

    private void updateQueueInFirebase(String doctorName, List<String> queue) {
        DatabaseReference doctorQueueRef = queueRef.child(doctorName).child("activeQueue");

        // Clear existing queue
        doctorQueueRef.removeValue().addOnSuccessListener(aVoid -> {
            // Add updated queue
            for (int i = 0; i < queue.size(); i++) {
                doctorQueueRef.child(String.valueOf(i)).setValue(queue.get(i));
            }

            // Update current serving token (first in queue)
            if (!queue.isEmpty()) {
                queueRef.child(doctorName).child("currentServing").setValue(queue.get(0));
            }
        });
    }

    private void updateAllTokenPositions(String doctorName, List<String> queue, String specialty) {
        for (int i = 0; i < queue.size(); i++) {
            String tokenId = queue.get(i);
            int position = i + 1;
            boolean isNext = (i == 0); // First in queue is next to be served
            boolean isCurrent = (i == 0); // First is currently being served

            updateTokenPosition(tokenId, position, queue.size(), isNext, isCurrent, specialty);
        }
    }

    private void updateTokenPosition(String tokenId, int position, int totalInQueue,
                                     boolean isNext, boolean isCurrent, String specialty) {
        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/TokenTracking")
                .child(tokenId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("queuePosition", position);
        updates.put("totalInQueue", totalInQueue);
        updates.put("isNextInQueue", isNext);
        updates.put("estimatedWaitTime", calculateWaitTime(position, specialty));
        updates.put("nextCallEstimate", calculateNextCallEstimate(position));

        // If this token is currently being served, update its status
        if (isCurrent) {
            updates.put("overallStatus", "in_progress");
            updates.put("currentStage", "consultation");
        }

        tokenRef.updateChildren(updates);

        // Log for debugging
        Log.d("QUEUE_UPDATE", "Token " + tokenId + " - Position: " + position +
                "/" + totalInQueue + ", Next: " + isNext + ", Current: " + isCurrent);
    }

    private String calculateWaitTime(int position, String specialty) {
        int baseMinutes;

        // Different specialties have different consultation times
        switch (specialty.toLowerCase()) {
            case "cardiology":
                baseMinutes = 20;
                break;
            case "surgery":
                baseMinutes = 25;
                break;
            case "pediatrics":
                baseMinutes = 15;
                break;
            default:
                baseMinutes = 15;
        }

        if (position == 1) {
            return "1-5 minutes";
        } else {
            int estimatedMinutes = (position - 1) * baseMinutes;
            return estimatedMinutes + " minutes";
        }
    }

    private String calculateNextCallEstimate(int position) {
        if (position == 1) return "Imminent";
        else if (position == 2) return "5-10 minutes";
        else if (position <= 5) return "10-30 minutes";
        else return "30+ minutes";
    }

    // Method to move to next patient (called by hospital staff)
    public void serveNextPatient(String doctorName) {
        DatabaseReference doctorQueueRef = queueRef.child(doctorName).child("activeQueue");

        doctorQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> queue = new ArrayList<>();

                // Get current queue
                for (DataSnapshot tokenSnap : snapshot.getChildren()) {
                    String token = tokenSnap.getValue(String.class);
                    if (token != null) {
                        queue.add(token);
                    }
                }

                if (!queue.isEmpty()) {
                    // Remove the first patient (currently being served)
                    String servedToken = queue.remove(0);

                    // Mark served token as completed
                    markTokenAsServed(servedToken);

                    // Update queue
                    updateQueueInFirebase(doctorName, queue);
                    updateAllTokenPositions(doctorName, queue, "General");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("QUEUE_MANAGER", "Error serving next patient: " + error.getMessage());
            }
        });
    }

    private void markTokenAsServed(String tokenId) {
        DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                .getReference("MbararaHospital/TokenTracking")
                .child(tokenId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("overallStatus", "completed");
        updates.put("currentStage", "completed");

        tokenRef.updateChildren(updates);
    }
}