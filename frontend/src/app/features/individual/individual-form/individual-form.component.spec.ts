import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IndividualFormComponent } from './individual-form.component';

describe('IndividualFormComponent', () => {
  let component: IndividualFormComponent;
  let fixture: ComponentFixture<IndividualFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IndividualFormComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(IndividualFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
