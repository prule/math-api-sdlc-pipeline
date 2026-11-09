package com.paulrule.mathapi;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/** STANDARDS.md §2. Every layer has a class, so a rule that matches nothing fails instead of passing vacuously. */
@AnalyzeClasses(packages = "com.paulrule.mathapi")
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain")
            .definedBy("..domain..")
            .layer("Application")
            .definedBy("..application..")
            .layer("Adapter")
            .definedBy("..adapter..")
            .layer("Config")
            .definedBy("..config..")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Adapter", "Config")
            .whereLayer("Adapter")
            .mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Config")
            .mayNotBeAccessedByAnyLayer();

    @ArchTest
    static final ArchRule domainDependsOnNoFramework = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "jakarta..", "com.fasterxml..", "tools.jackson..");

    @ArchTest
    static final ArchRule applicationDependsOnNoWebOrAdapter = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", "..adapter..");

    @Test
    void domainRuleRejectsAFrameworkDependentDomainClass() {
        var fixture = new ClassFileImporter().importPackages("com.paulrule.archfixture");

        assertThatThrownBy(() -> domainDependsOnNoFramework.check(fixture))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("FrameworkDependentDomainClass");
    }
}
