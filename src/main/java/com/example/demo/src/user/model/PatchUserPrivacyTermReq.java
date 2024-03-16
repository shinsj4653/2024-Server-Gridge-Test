package com.example.demo.src.user.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchUserPrivacyTermReq {
    private boolean serviceTerm;
    private boolean dataTerm;
    private boolean locationTerm;
}