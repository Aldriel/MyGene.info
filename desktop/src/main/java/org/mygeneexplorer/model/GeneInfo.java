/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mygeneexplorer.model;

import java.util.List;

/**
 * Gene summary returned by MyGene.info.
 *
 * @param id          MyGene.info identifier (usually the Entrez identifier)
 * @param symbol      official symbol, e.g. {@code BRCA1}
 * @param name        full gene name
 * @param entrezGene  NCBI Entrez Gene identifier, or {@code null} if absent
 * @param summary     functional summary, or {@code null} if absent
 * @param typeOfGene  gene type, e.g. {@code protein-coding}, or {@code null} if absent
 * @param mapLocation cytogenetic location, e.g. {@code 17q21.31}, or {@code null} if absent
 * @param aliases     alternative symbols, possibly empty
 */
public record GeneInfo(
        String id,
        String symbol,
        String name,
        Long entrezGene,
        String summary,
        String typeOfGene,
        String mapLocation,
        List<String> aliases
) {

    /** Defensive copy: the alias list stays immutable and is never {@code null}. */
    public GeneInfo {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    /** @return the Entrez identifier if known, otherwise the MyGene.info identifier */
    public String entrezIdOrId() {
        return entrezGene != null ? entrezGene.toString() : id;
    }
}
