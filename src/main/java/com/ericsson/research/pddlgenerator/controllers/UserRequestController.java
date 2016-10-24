package com.ericsson.research.pddlgenerator.controllers;

import com.ericsson.research.pddlgenerator.representations.FileInput;
import com.ericsson.research.pddlgenerator.representations.GeneratedFiles;
import com.ericsson.research.pddlgenerator.service.PDDLGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class UserRequestController {

    private final AtomicLong counter = new AtomicLong();

    @PostMapping("/submitInput")
    public ResponseEntity GeneratorService(@RequestParam("transitionsFile") MultipartFile transitionsFile,
                                          @RequestParam("statesFile") MultipartFile statesFile,
                                          @RequestParam("TRFile") MultipartFile trFile
                                        ){

        FileInput fileInput = new FileInput(
                readMultipartFile(transitionsFile),
                readMultipartFile(statesFile),
                readMultipartFile(trFile));

        if (fileInput.getTransitionsFile().isEmpty() || fileInput.getStatesFile().isEmpty()){
            return new ResponseEntity("Error: please supply both transition and state files!", HttpStatus.NOT_ACCEPTABLE);
        }

       // System.out.println("Got transitions file: "+fileInput.getTransitionsFile() + "\n\n\n");
       // System.out.println("Got states file: "+fileInput.getStatesFile());

        PDDLGenerator Generator = new PDDLGenerator();
        String[] result = Generator.GeneratePlan(
                fileInput.getStatesFile(),
                fileInput.getTransitionsFile(),
                fileInput.getTransformationRulesFile(),
                false);

        if (result != null)
            return new ResponseEntity(new GeneratedFiles(result[1], result[0]), HttpStatus.OK);
        else
            return null;
    }

    private String readMultipartFile(MultipartFile fileData){
        String data = "";
        try {
            InputStream inputStream = fileData.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                data = data + line + "\n";
            }

        }catch (Exception ex){
            ex.toString();
        }
        return data;
    }
}
