package com.biocommons.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.nio.charset.StandardCharsets;
import java.util.List;

// Prawidłowe importy dopasowane do struktury pl.poznan.put z GitHuba
import pl.poznan.put.pdb.analysis.PdbParser;
import pl.poznan.put.pdb.analysis.PdbModel;

@RestController
public class StructureController {

    private final Timer parseTimer;

    public StructureController(MeterRegistry registry) {
        this.parseTimer = Timer.builder("biocommons_parse_duration_seconds")
                .description("Czas potrzebny na sparsowanie struktury 3D")
                .register(registry);
    }

    @PostMapping("/parse")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {
        return parseTimer.record(() -> {
            try {
                // 1. Odczytujemy zawartość pliku przesłanego za pomocą curl.exe
                String structureContent = new String(file.getBytes(), StandardCharsets.UTF_8);

                // 2. Wywołujemy parser z biblioteki BioCommons
                PdbParser parser = new PdbParser();
                List<PdbModel> models = parser.parse(structureContent);

                if (models.isEmpty()) {
                    return "Błąd: Przesłany plik nie zawiera poprawnych modeli PDB.";
                }

                PdbModel firstModel = models.get(0);

                // 3. Budujemy tekstową odpowiedź z metadanymi struktury
                StringBuilder response = new StringBuilder();
                response.append("PDB id: ").append(firstModel.idCode()).append("\n");
                response.append("Title: ").append(firstModel.title()).append("\n");
                response.append("Classification: ").append(firstModel.header().classification()).append("\n");
                response.append("Resolution: ").append(firstModel.resolution().resolution()).append("\n");
                
                return response.toString();

            } catch (Exception e) {
                return "Wystąpił błąd podczas przetwarzania pliku przez BioCommons: " + e.getMessage();
            }
        });
    }
}